#!/usr/bin/env bash
# =============================================================================
# 企业 AI 应用市场 — 应用层服务器初始化脚本
# 目标机器：App-AI-Test / 192.168.1.132  (Ubuntu 24.04 LTS)
# 职责：JDK17 运行时 + Spring Boot 后端 + Nginx（工作台:80 / 运营后台:8081）
#
# 用法（在该服务器上以 root 执行）：
#   sudo CRED_FILE=/tmp/.aitest-cred bash /aitest/packages/deploy/scripts/setup-app-server.sh
#
# CRED_FILE 需包含两行（由数据层的 /aitest/mysql/.credentials 提供）：
#   DB_PASSWORD=xxx
#   REDIS_PASSWORD=xxx
# 脚本读完即可删除；JWT_SECRET 由本机自动生成并写入 env 文件。
#
# 前置条件：发布产物已上传到位
#   /aitest/app/releases/ai-marketplace.jar
#   /aitest/www/workbench/index.html
#   /aitest/www/admin/index.html
# =============================================================================
set -euo pipefail

BASE=/aitest
BUNDLE="${BUNDLE:-/aitest/packages/deploy}"
CRED_FILE="${CRED_FILE:-/tmp/.aitest-cred}"
DATA_SERVER_IP="${DATA_SERVER_IP:-192.168.1.131}"
APP_PORT=8080
ADMIN_PORT=8081
ENV_FILE="$BASE/app/config/ai-marketplace.env"

log()  { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }
warn() { printf '\033[1;33m[警告] %s\033[0m\n' "$*" >&2; }
die()  { printf '\033[1;31m[失败] %s\033[0m\n' "$*" >&2; exit 1; }
genpass() { tr -dc 'A-Za-z0-9' </dev/urandom | head -c "${1:-48}"; }

[[ $EUID -eq 0 ]] || die "请用 sudo 运行本脚本"
[[ -d $BUNDLE ]]  || die "找不到部署包目录 $BUNDLE"

# ---------------------------------------------------------------------------
log "1/7 校验发布产物是否已上传"
# ---------------------------------------------------------------------------
[[ -f $BASE/app/releases/ai-marketplace.jar ]] || die "缺少 $BASE/app/releases/ai-marketplace.jar"
[[ -f $BASE/www/workbench/index.html ]]        || die "缺少工作台前端产物 $BASE/www/workbench/index.html"
[[ -f $BASE/www/admin/index.html ]]            || die "缺少运营后台前端产物 $BASE/www/admin/index.html"
command -v java >/dev/null || die "未安装 java，请先 apt-get install openjdk-17-jre-headless"
mkdir -p "$BASE"/{packages,scripts,backup} "$BASE"/app/{config,logs} \
         "$BASE"/www/{workbench,admin} "$BASE"/nginx/{conf,logs} "$BASE"/storage/files
echo "    jar $(du -h "$BASE/app/releases/ai-marketplace.jar" | cut -f1)，java $(java -version 2>&1 | head -1 | cut -d'"' -f2)  ✓"

# ---------------------------------------------------------------------------
log "2/7 预检：到数据层 $DATA_SERVER_IP 的 3306 / 6379 连通性"
# ---------------------------------------------------------------------------
for port in 3306 6379; do
    if timeout 5 bash -c "cat </dev/null >/dev/tcp/$DATA_SERVER_IP/$port" 2>/dev/null; then
        echo "    $DATA_SERVER_IP:$port 可达  ✓"
    else
        die "$DATA_SERVER_IP:$port 不可达——请先在数据层执行 setup-data-server.sh（含 ufw 放行）"
    fi
done

# ---------------------------------------------------------------------------
log "3/7 写入运行时环境变量 $ENV_FILE"
# ---------------------------------------------------------------------------
[[ -f $CRED_FILE ]] || die "找不到口令文件 $CRED_FILE，请先把数据层的 .credentials 传过来"
# shellcheck disable=SC1090
source "$CRED_FILE"
[[ -n ${DB_PASSWORD:-} ]]    || die "$CRED_FILE 中缺少 DB_PASSWORD"
[[ -n ${REDIS_PASSWORD:-} ]] || die "$CRED_FILE 中缺少 REDIS_PASSWORD"

# JWT 密钥首次生成后固定下来；重复执行脚本不会更换，否则已签发的 token 会全部失效
if [[ -f $ENV_FILE ]] && grep -q '^JWT_SECRET=.\+' "$ENV_FILE"; then
    JWT_SECRET=$(grep '^JWT_SECRET=' "$ENV_FILE" | cut -d= -f2-)
    warn "沿用已有 JWT_SECRET（更换会导致所有已登录用户掉线）"
else
    JWT_SECRET=$(genpass 48)
fi

install -m 0640 -o aiuser -g aiuser "$BUNDLE/env/ai-marketplace.env.template" "$ENV_FILE"
sed -i "s|__DB_PASSWORD__|$DB_PASSWORD|; s|__REDIS_PASSWORD__|$REDIS_PASSWORD|; s|__JWT_SECRET__|$JWT_SECRET|" "$ENV_FILE"
chmod 600 "$ENV_FILE"
chown aiuser:aiuser "$ENV_FILE"
grep -q '__.*__' "$ENV_FILE" && die "env 文件中仍有未替换的占位符"
echo "    已写入（权限 600，属主 aiuser），口令不回显  ✓"

# ---------------------------------------------------------------------------
log "4/7 注册 systemd 服务 ai-marketplace"
# ---------------------------------------------------------------------------
install -m 0644 "$BUNDLE/systemd/ai-marketplace.service" /etc/systemd/system/ai-marketplace.service
systemctl daemon-reload
systemctl enable ai-marketplace >/dev/null 2>&1
systemctl restart ai-marketplace

echo -n "    等待应用启动"
for i in $(seq 1 60); do
    if grep -q "Started AiMarketplaceApplication" "$BASE/app/logs/stdout.log" 2>/dev/null; then
        echo " 完成（${i}s）✓"; break
    fi
    systemctl is-active --quiet ai-marketplace || {
        echo; tail -40 "$BASE/app/logs/stderr.log" "$BASE/app/logs/stdout.log" 2>/dev/null >&2
        die "应用启动失败，上面是日志"
    }
    echo -n "."; sleep 1
    [[ $i -eq 60 ]] && { echo; tail -40 "$BASE/app/logs/stdout.log" >&2; die "60 秒内未启动完成"; }
done
ss -ltn "sport = :$APP_PORT" | grep -q "$APP_PORT" || die "端口 $APP_PORT 未监听"

# ---------------------------------------------------------------------------
log "5/7 配置 Nginx（工作台 :80 / 运营后台 :$ADMIN_PORT）"
# ---------------------------------------------------------------------------
install -m 0644 "$BUNDLE/nginx/workbench.conf" "$BASE/nginx/conf/workbench.conf"
install -m 0644 "$BUNDLE/nginx/admin.conf"     "$BASE/nginx/conf/admin.conf"

# 真实配置留在 /aitest，sites-enabled 里只放软链
ln -sfn "$BASE/nginx/conf/workbench.conf" /etc/nginx/sites-available/workbench.conf
ln -sfn "$BASE/nginx/conf/admin.conf"     /etc/nginx/sites-available/admin.conf
ln -sfn /etc/nginx/sites-available/workbench.conf /etc/nginx/sites-enabled/workbench.conf
ln -sfn /etc/nginx/sites-available/admin.conf     /etc/nginx/sites-enabled/admin.conf
# 发行版默认站点占用 80 且是 default_server，必须移除
rm -f /etc/nginx/sites-enabled/default

# www-data 需要能读前端产物、能 traverse /aitest
chmod 755 "$BASE" "$BASE/www" "$BASE/www/workbench" "$BASE/www/admin"
chmod -R a+rX "$BASE/www"
chown -R root:adm "$BASE/nginx/logs"; chmod 755 "$BASE/nginx/logs"

nginx -t || die "Nginx 配置校验失败"
systemctl enable nginx >/dev/null 2>&1
systemctl restart nginx

# ---------------------------------------------------------------------------
log "6/7 配置防火墙"
# ---------------------------------------------------------------------------
if command -v ufw >/dev/null; then
    ufw allow 22/tcp                comment 'SSH'
    ufw allow 80/tcp                comment 'AI Marketplace workbench'
    ufw allow "$ADMIN_PORT"/tcp     comment 'AI Marketplace admin console'
    ufw --force enable
    ufw status verbose | sed 's/^/    /'
else
    warn "未安装 ufw，跳过"
fi

# ---------------------------------------------------------------------------
log "7/7 本机验收"
# ---------------------------------------------------------------------------
code_api=$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$APP_PORT/api/auth/oauth-url?redirectUri=x" || echo 000)
code_web=$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1/ || echo 000)
code_adm=$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$ADMIN_PORT/" || echo 000)
code_pxy=$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1/api/auth/oauth-url?redirectUri=x" || echo 000)

cat <<EOF

=============================================================================
应用层部署完成，本机验收结果

  systemd ai-marketplace : $(systemctl is-active ai-marketplace)  (enabled=$(systemctl is-enabled ai-marketplace))
  后端直连 :$APP_PORT/api    : HTTP $code_api
  Nginx 反代 :80/api        : HTTP $code_pxy
  工作台    :80              : HTTP $code_web
  运营后台  :$ADMIN_PORT              : HTTP $code_adm

  访问入口
    员工工作台  http://$DATA_SERVER_IP 的同网段浏览器打开 → http://192.168.1.132/
    运营后台    http://192.168.1.132:$ADMIN_PORT/
    接口文档    生产 profile 下已被 knife4j.production=true 关闭

  常用命令
    systemctl status ai-marketplace
    journalctl -u ai-marketplace -f
    tail -f $BASE/app/logs/stdout.log
    nginx -t && systemctl reload nginx

  目录约定
    $BASE/app/releases   后端 jar（发版覆盖此文件后 systemctl restart ai-marketplace）
    $BASE/app/config     ai-marketplace.env（含明文口令，权限 600）
    $BASE/app/logs       应用日志
    $BASE/www            两个前端产物
    $BASE/nginx          站点配置与访问日志
    $BASE/storage/files  作品包落盘目录
=============================================================================
EOF

[[ $code_web == 200 && $code_adm == 200 ]] || warn "前端页面返回码异常，请检查 Nginx 配置"
