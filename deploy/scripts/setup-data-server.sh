#!/usr/bin/env bash
# =============================================================================
# 企业 AI 应用市场 — 数据层服务器初始化脚本
# 目标机器：Mysql-AI-Test / 192.168.1.131  (Ubuntu 24.04 LTS)
# 职责：MySQL 8 + Redis 7，数据/配置/日志全部收敛到 /aitest
#
# 用法（在该服务器上以 root 执行）：
#   sudo BUNDLE=/aitest/packages/deploy bash /aitest/packages/deploy/scripts/setup-data-server.sh
#
# 脚本是幂等的，可以重复执行；重复执行不会重置已有口令，也不会重复导入数据。
# =============================================================================
set -euo pipefail

BASE=/aitest
BUNDLE="${BUNDLE:-/aitest/packages/deploy}"
APP_SERVER_IP="${APP_SERVER_IP:-192.168.1.132}"
DB_NAME=ai_marketplace
DB_USER=ai_market
CRED_FILE="$BASE/mysql/.credentials"

log()  { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }
warn() { printf '\033[1;33m[警告] %s\033[0m\n' "$*" >&2; }
die()  { printf '\033[1;31m[失败] %s\033[0m\n' "$*" >&2; exit 1; }

# 只生成字母数字，避免在 .env / SQL / shell 里出现转义问题
genpass() { tr -dc 'A-Za-z0-9' </dev/urandom | head -c "${1:-24}"; }

[[ $EUID -eq 0 ]] || die "请用 sudo 运行本脚本"
[[ -d $BUNDLE ]]  || die "找不到部署包目录 $BUNDLE，请先上传 deploy/ 到该路径"

# ---------------------------------------------------------------------------
log "1/8 创建 $BASE 目录骨架"
# ---------------------------------------------------------------------------
mkdir -p "$BASE"/{packages,scripts,sql}
mkdir -p "$BASE"/mysql/{data,conf,logs,backup}
mkdir -p "$BASE"/redis/{data,conf,logs}
# 部署包本身也留一份在 /aitest，便于日后在服务器上直接查配置
[[ "$(cd "$BUNDLE" && pwd)" != "$BASE/packages/deploy" ]] && \
    { mkdir -p "$BASE/packages/deploy"; cp -r "$BUNDLE"/. "$BASE/packages/deploy/"; }
BUNDLE="$BASE/packages/deploy"

# ---------------------------------------------------------------------------
log "2/8 生成并保存服务口令"
# ---------------------------------------------------------------------------
if [[ -f $CRED_FILE ]]; then
    warn "口令文件已存在，沿用其中的口令（不会重新生成）"
else
    # umask 必须限制在子 shell 内，否则会泄漏到后续创建的配置文件上，
    # 导致 mysqld（以 mysql 用户运行）读不到 /etc/mysql 下的 include 指针文件
    (
        umask 077
        cat > "$CRED_FILE" <<EOF
# 本文件由 setup-data-server.sh 生成，仅 root 可读。
# 应用服务器需要用到其中的值来生成 /aitest/app/config/ai-marketplace.env
DB_PASSWORD=$(genpass 24)
REDIS_PASSWORD=$(genpass 24)
EOF
    )
    chmod 600 "$CRED_FILE"
fi
# shellcheck disable=SC1090
source "$CRED_FILE"
[[ -n ${DB_PASSWORD:-} && -n ${REDIS_PASSWORD:-} ]] || die "口令读取失败"

# ---------------------------------------------------------------------------
log "3/8 迁移 MySQL 数据目录到 $BASE/mysql/data"
# ---------------------------------------------------------------------------
systemctl stop mysql

if [[ -d $BASE/mysql/data/mysql ]]; then
    warn "数据目录已迁移过，跳过 rsync"
else
    [[ -d /var/lib/mysql/mysql ]] || die "/var/lib/mysql 为空，MySQL 可能尚未初始化"
    rsync -aHAX --info=stats0 /var/lib/mysql/ "$BASE/mysql/data/"
fi
chown -R mysql:mysql "$BASE/mysql/data"
chmod 750 "$BASE/mysql/data"
chown -R mysql:mysql "$BASE/mysql/logs"

# ---------------------------------------------------------------------------
log "4/8 配置 AppArmor 与 MySQL 配置文件"
# ---------------------------------------------------------------------------
# Ubuntu 的 mysqld 受 AppArmor 约束，默认只允许访问 /var/lib/mysql。
# 主 profile 末尾有 #include <local/usr.sbin.mysqld>，把放行规则写到 local 片段里，
# 这样既不用改主 profile，也不会在 mysql 包升级时被覆盖。
install -m 0644 "$BUNDLE/mysql/apparmor-local-usr.sbin.mysqld" \
        /etc/apparmor.d/local/usr.sbin.mysqld
if systemctl is-active --quiet apparmor; then
    apparmor_parser -r /etc/apparmor.d/usr.sbin.mysqld
else
    warn "apparmor 服务未运行，跳过 profile 重载"
fi

install -m 0644 "$BUNDLE/mysql/mysqld-aitest.cnf" "$BASE/mysql/conf/mysqld-aitest.cnf"
# zzz 前缀确保在发行版默认 mysqld.cnf 之后加载，从而覆盖 datadir / bind-address
cat > /etc/mysql/mysql.conf.d/zzz-aitest.cnf <<EOF
# 由 setup-data-server.sh 生成：真实配置放在 /aitest 下便于统一管理与备份
!include $BASE/mysql/conf/mysqld-aitest.cnf
EOF
# systemd 是先切到 mysql 用户再 exec mysqld 的，而 !includedir 对不可读文件静默跳过，
# 因此这个指针文件必须对 mysql 用户可读，否则整套配置会被无声忽略
chmod 0644 /etc/mysql/mysql.conf.d/zzz-aitest.cnf

# 以 mysql 用户身份预演一次配置解析，确保不是只有 root 才读得到
sudo -u mysql mysqld --print-defaults 2>/dev/null | tr ' ' '\n' \
    | grep -q "^--datadir=$BASE/mysql/data$" \
    || die "mysql 用户读不到新 datadir，请检查 /etc/mysql/mysql.conf.d/zzz-aitest.cnf 与 $BASE/mysql/conf/ 的权限"

systemctl start mysql
sleep 3
systemctl is-active --quiet mysql || {
    tail -30 "$BASE/mysql/logs/error.log" >&2 || true
    die "MySQL 启动失败，上面是错误日志"
}
ACTUAL_DATADIR=$(mysql -N -B -e "SELECT @@datadir" 2>/dev/null | tr -d '\r')
[[ "$ACTUAL_DATADIR" == "$BASE/mysql/data/" ]] \
    || die "datadir 未生效，实际为 '$ACTUAL_DATADIR'"
echo "    datadir = $ACTUAL_DATADIR  ✓"

# ---------------------------------------------------------------------------
log "5/8 导入表结构与初始管理员"
# ---------------------------------------------------------------------------
[[ -f "$BASE/sql/schema.sql" ]] || cp "$BUNDLE/../../backend/src/main/resources/sql/schema.sql" "$BASE/sql/" 2>/dev/null \
    || cp "$BUNDLE/sql/schema.sql" "$BASE/sql/" 2>/dev/null \
    || die "找不到 schema.sql，请把它放到 $BASE/sql/"
cp -f "$BUNDLE/sql/01-init-admin.sql" "$BASE/sql/"

mysql < "$BASE/sql/schema.sql"
TABLES=$(mysql -N -B -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME'")
[[ "$TABLES" -ge 16 ]] || die "表数量异常，期望 >=16，实际 $TABLES"
echo "    $DB_NAME 表数量 = $TABLES  ✓"

mysql < "$BASE/sql/01-init-admin.sql" >/dev/null

# ---------------------------------------------------------------------------
log "6/8 创建应用数据库账号（仅允许应用服务器连入）"
# ---------------------------------------------------------------------------
mysql <<SQL
CREATE USER IF NOT EXISTS '$DB_USER'@'$APP_SERVER_IP' IDENTIFIED BY '$DB_PASSWORD';
ALTER  USER                '$DB_USER'@'$APP_SERVER_IP' IDENTIFIED BY '$DB_PASSWORD';
CREATE USER IF NOT EXISTS '$DB_USER'@'localhost'       IDENTIFIED BY '$DB_PASSWORD';
ALTER  USER                '$DB_USER'@'localhost'       IDENTIFIED BY '$DB_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER,
      CREATE TEMPORARY TABLES, LOCK TABLES, EXECUTE, CREATE VIEW,
      SHOW VIEW, CREATE ROUTINE, ALTER ROUTINE, TRIGGER
  ON \`$DB_NAME\`.* TO '$DB_USER'@'$APP_SERVER_IP';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER,
      CREATE TEMPORARY TABLES, LOCK TABLES, EXECUTE, CREATE VIEW,
      SHOW VIEW, CREATE ROUTINE, ALTER ROUTINE, TRIGGER
  ON \`$DB_NAME\`.* TO '$DB_USER'@'localhost';
FLUSH PRIVILEGES;
SQL
echo "    账号 $DB_USER@$APP_SERVER_IP 就绪  ✓"

# ---------------------------------------------------------------------------
log "7/8 配置 Redis（数据与配置迁到 $BASE/redis）"
# ---------------------------------------------------------------------------
install -m 0750 -o redis -g redis -d "$BASE/redis/data" "$BASE/redis/logs"
sed "s/__REDIS_PASSWORD__/$REDIS_PASSWORD/" "$BUNDLE/redis/redis-aitest.conf" \
    > "$BASE/redis/conf/redis.conf"
chmod 640 "$BASE/redis/conf/redis.conf"
chown redis:redis "$BASE/redis/conf/redis.conf"

# 发行版 unit 带 ProtectSystem=strict，必须用 drop-in 放行 /aitest/redis 并改 ExecStart
mkdir -p /etc/systemd/system/redis-server.service.d
install -m 0644 "$BUNDLE/redis/redis-server-aitest.override" \
        /etc/systemd/system/redis-server.service.d/aitest.conf
systemctl daemon-reload
systemctl restart redis-server
sleep 2
systemctl is-active --quiet redis-server || {
    journalctl -u redis-server -n 30 --no-pager >&2 || true
    die "Redis 启动失败，上面是日志"
}
redis-cli -a "$REDIS_PASSWORD" --no-auth-warning ping | grep -q PONG \
    || die "Redis 口令认证未通过"
REDIS_DIR=$(redis-cli -a "$REDIS_PASSWORD" --no-auth-warning config get dir | tail -1 | tr -d '\r')
[[ "$REDIS_DIR" == "$BASE/redis/data" ]] || die "Redis dir 未生效，实际为 '$REDIS_DIR'"
echo "    dir = $REDIS_DIR，PING 正常  ✓"

# ---------------------------------------------------------------------------
log "8/8 配置防火墙（仅对应用服务器开放 3306 / 6379）"
# ---------------------------------------------------------------------------
if command -v ufw >/dev/null; then
    # 先放行 SSH 再 enable，避免把自己锁在外面
    ufw allow 22/tcp comment 'SSH'
    ufw allow from "$APP_SERVER_IP" to any port 3306 proto tcp comment 'MySQL from app server'
    ufw allow from "$APP_SERVER_IP" to any port 6379 proto tcp comment 'Redis from app server'
    ufw --force enable
    ufw status verbose | sed 's/^/    /'
else
    warn "未安装 ufw，跳过防火墙配置——请自行确认 3306/6379 的访问控制"
fi

cat <<EOF

=============================================================================
数据层初始化完成

  MySQL   : $(mysql -N -B -e 'SELECT VERSION()')   datadir=$BASE/mysql/data
  Redis   : $(redis-server --version | awk '{print $3}' | tr -d 'v=')   dir=$BASE/redis/data
  数据库  : $DB_NAME（$TABLES 张表，已含初始管理员 admin）
  应用账号: $DB_USER  仅允许 $APP_SERVER_IP 与 localhost 连入
  口令文件: $CRED_FILE  (权限 600，仅 root 可读)

下一步：在应用服务器上执行 setup-app-server.sh，
        它会从本机 $CRED_FILE 读取口令写入 /aitest/app/config/ai-marketplace.env
=============================================================================
EOF
