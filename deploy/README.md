# 企业 AI 应用市场 — 内网部署手册

部署完成时间：2026-09-21　｜　最近更新：2026-09-23（启用 HTTPS、登录链路打通）
状态：基础设施、应用进程、登录（账号口令）均已验收通过；企微 SSO 待可信域名配置，运营后台接口待补（见 `docs/KNOWN-GAPS.md`）

---

## 一、部署拓扑

```
                         ┌──────────────────────────────────────┐
   员工浏览器 ──80/443───►│  App-AI-Test  192.168.1.132 (16G)    │
   运营浏览器 ─8081/8443─►│  Ubuntu 24.04 · 应用层                │
                         │                                      │
                         │  Nginx 1.24（TLS: *.tri-ibiotech.com）│
                         │    :80/:443    → /aitest/www/workbench│
                         │    :8081/:8443 → /aitest/www/admin    │
                         │    /api/       → 127.0.0.1:8080       │
                         │  Spring Boot 3.3.4 (systemd)         │
                         │    ai-marketplace.service :8080      │
                         │    context-path=/api                 │
                         └───────────────┬──────────────────────┘
                                         │ 3306 / 6379
                                         │ (ufw 仅放行 132)
                         ┌───────────────▼──────────────────────┐
                         │  Mysql-AI-Test 192.168.1.131 (8G)    │
                         │  Ubuntu 24.04 · 数据层                │
                         │                                      │
                         │  MySQL 8.0.46   datadir=/aitest/...  │
                         │  Redis 7.0.15   dir=/aitest/...      │
                         │  每日 02:00 自动备份，保留 14 天        │
                         └──────────────────────────────────────┘
```

> 同一台 132 上还跑着另一个不属于本项目的服务：`/aitest/tri-meeting/`（监听 127.0.0.1:8090，
> 由 Nginx 8082 反代）。本项目的脚本不会触碰它，改动 Nginx 时注意别覆盖 8082 的站点配置。

**为什么运营后台用独立端口 8081 而不是 README 里写的 `/admin` 子路径：**
`frontend-admin/vite.config.ts` 没设 `base`，router 用的是无参 `createWebHistory()`，
构建产物里的资源引用是 `/assets/...` 绝对路径，挂到子路径会全站白屏。
独立端口 + 根路径部署可以零代码改动解决。详见 `nginx/admin.conf` 顶部注释。

---

## 二、目录约定（`/aitest`）

### 数据层 131

| 路径 | 用途 |
|---|---|
| `/aitest/mysql/data` | MySQL datadir（已迁移，非默认的 `/var/lib/mysql`） |
| `/aitest/mysql/conf/mysqld-aitest.cnf` | MySQL 真实配置，由 `/etc/mysql/mysql.conf.d/zzz-aitest.cnf` 用 `!include` 引入 |
| `/aitest/mysql/logs` | error.log / slow.log |
| `/aitest/mysql/backup` | 每日备份产物 |
| `/aitest/mysql/.credentials` | **DB 与 Redis 口令，权限 600 仅 root** |
| `/aitest/redis/{conf,data,logs}` | Redis 配置、RDB、日志 |
| `/aitest/sql` | `schema.sql` + `01-init-admin.sql` |
| `/aitest/scripts/backup-db.sh` | 备份脚本（已挂 root crontab） |
| `/aitest/packages/deploy` | 部署包留档 |

### 应用层 132

| 路径 | 用途 |
|---|---|
| `/aitest/app/releases/ai-marketplace.jar` | 后端 jar（发版即覆盖此文件） |
| `/aitest/app/config/ai-marketplace.env` | **运行时环境变量，含明文口令，权限 600 属主 aiuser** |
| `/aitest/app/logs/stdout.log` | 应用日志（systemd append 模式） |
| `/aitest/www/workbench` | 员工工作台静态产物 |
| `/aitest/www/admin` | 运营后台静态产物 |
| `/aitest/nginx/conf` | 两个站点配置（`sites-enabled` 里是软链） |
| `/aitest/nginx/logs` | 访问与错误日志 |
| `/aitest/storage/files` | 作品包落盘目录，按 `{yyyyMMdd}/{uuid}{ext}` 组织 |
| `/aitest/scripts/deploy-release.sh` | 发版脚本 |

> 中间件二进制仍由 apt 安装在 `/usr`（便于 systemd 管理与安全更新），
> 但**所有数据、配置、日志、Web 根目录、上传文件、备份、脚本都收敛在 `/aitest`**。
> 迁移或快照时只需带走 `/aitest` 加上 `/etc` 下的少量指针文件。

---

## 三、访问入口

| 用途 | 地址 |
|---|---|
| 员工工作台 | http://192.168.1.132/ 　https://192.168.1.132/ |
| 运营后台 | http://192.168.1.132:8081/ 　https://192.168.1.132:8443/ |
| 后端（内部） | http://192.168.1.132:8080/api |
| 接口文档 | 生产 profile 下已被 `knife4j.production=true` 关闭 |

> 证书是 `*.tri-ibiotech.com` 通配符证书，**用 IP 访问 HTTPS 会有证书名不匹配告警**，
> 这是预期行为。等正式域名（如 `ai.tri-ibiotech.com`）解析并回源到本机后，
> 用域名访问即为绿色小锁。当前 80/8081 的 HTTP 入口未做强制跳转，
> 原因见 `nginx/workbench.conf` 里的注释（WAF 以 HTTP 回源时强制跳转会形成回环）。

公网侧现状：`121.46.250.190:8081` 已存在到 132:8081 的端口映射（实测可直接访问运营后台），
而 `:80/:443` 指向的是另一个 nginx（公司官网方向），并非本机。
若要从公网提供本服务，需网络侧新增到 132 的 443 映射。

---

## 四、凭据与密钥

全部为部署时随机生成，**未在本文档中记录**，请按下列位置查看：

| 项 | 位置 |
|---|---|
| MySQL 应用账号口令 | 132 上 `/aitest/app/config/ai-marketplace.env` 的 `DB_PWD` |
| Redis 口令 | 同上，`REDIS_PWD` |
| JWT 签名密钥 | 同上，`JWT_SECRET`（更换会导致所有已登录用户掉线） |
| 上述口令的源头副本 | 131 上 `/aitest/mysql/.credentials`（root only） |

数据库账号：`ai_market`，仅允许从 `192.168.1.132` 与 `localhost` 连入，
只授予 `ai_marketplace` 库的 DML+DDL 权限，未授予 `GRANT OPTION`、`SUPER` 等。
MySQL `root` 保持 Ubuntu 默认的 `auth_socket`，只能本机 `sudo mysql` 登录。

初始管理员：`sys_user` 中 `wecom_userid='admin'`、`account='admin'`、`roles='ADMIN,OPERATOR,USER'`。

**本地登录账号**：`admin`，初始口令在部署时随机生成并已当面告知，
**请首次登录后立即修改**。当前 `AUTH_LOCAL_LOGIN_ENABLED=true`（企微可信域名未配好前的过渡措施），
正式环境必须改回 `false`，登录只走企微 SSO。

修改口令的方法——先生成 BCrypt 哈希（开发机需 JDK17 与 hutool-all jar，
必须用 Hutool 自己的实现，`htpasswd` 产出的 `$2y$` 前缀它的 `checkpw` 不一定认）：

```bash
cat > /tmp/Hash.java <<'EOF'
import cn.hutool.crypto.digest.BCrypt;
public class Hash { public static void main(String[] a){ System.out.print(BCrypt.hashpw(a[0])); } }
EOF
HJ=~/.m2/repository/cn/hutool/hutool-all/5.8.32/hutool-all-5.8.32.jar
javac -cp "$(cygpath -w $HJ)" -d /tmp/h /tmp/Hash.java
java  -cp "/tmp/h;$(cygpath -w $HJ)" Hash '新口令'      # Windows 下 classpath 用 ; 且需 Windows 路径
```

再在 131 上写库：

```bash
sudo mysql -e "UPDATE ai_marketplace.sys_user SET password_hash='<新哈希>' WHERE account='admin';"
```

**企微凭据**已写入 132 的 env 文件（`WECOM_CORP_ID` / `WECOM_AGENT_ID` / `WECOM_APP_SECRET`），
实测 `gettoken` 返回 `errcode 0`，说明凭据有效且出口 IP `121.46.250.190` 已在企业可信 IP 白名单内。
凭据只存在于服务器 env 文件，**不要写进 `application.yml` 或 env 模板**（那等于泄露给所有能读代码的人）。

**TLS 证书**：`/aitest/nginx/cert/tri-ibiotech.com.{pem,key}`（key 权限 600 属主 root）。
通配符 `*.tri-ibiotech.com`，含完整三级证书链，**到期日 2027-01-07**，续期后需 `systemctl reload nginx`。

**企微域名归属验证文件**：`WW_verify_61IR393ntYc1jRjW.txt` 已放在两个站点根目录，
Nginx 有专门的 location 放行。注意 `deploy-release.sh` 发版会清空 `www/` 目录，
**发版后需重新放置该文件**（setup 脚本里也有同样一步）。

---

## 五、重新部署 / 发版

### 5.1 在开发机构建发布包

```powershell
# 前置：JDK17 在 C:\Program Files\Java\jdk-17，Maven 在 D:\WorkbuddyWorkspace\tools\apache-maven-3.9.16
# 注意 JAVA_HOME 环境变量当前指向 JDK 1.8，脚本内部已覆盖
powershell -File D:\WorkbuddyWorkspace\ai-marketplace\deploy\build-release.ps1
```

产物：`dist-release\ai-marketplace-release-<时间戳>.tar.gz`

### 5.2 上传并发版

```bash
scp dist-release/ai-marketplace-release-*.tar.gz aiuser@192.168.1.132:/tmp/
ssh aiuser@192.168.1.132
sudo bash /aitest/scripts/deploy-release.sh /tmp/ai-marketplace-release-*.tar.gz
```

`deploy-release.sh` 会自动把上一版 jar 备份为 `ai-marketplace.jar.prev-<时间戳>`（保留最近 3 个），
并在启动失败时打印回滚命令。

### 5.3 只改前端

把新的 `dist/` 覆盖到 `/aitest/www/workbench` 或 `/aitest/www/admin`，
然后 `sudo systemctl reload nginx` 即可，无需重启后端。

### 5.4 改了数据库结构

```bash
# 把变更 SQL 放到 131 的 /aitest/sql/，然后
ssh aiuser@192.168.1.131
sudo mysql < /aitest/sql/<变更脚本>.sql
```

---

## 六、日常运维

### 应用层 132

```bash
systemctl status ai-marketplace          # 服务状态
sudo journalctl -u ai-marketplace -f     # systemd 视角日志
tail -f /aitest/app/logs/stdout.log      # 应用日志
sudo systemctl restart ai-marketplace    # 重启
sudo nginx -t && sudo systemctl reload nginx
tail -f /aitest/nginx/logs/workbench-access.log
```

### 数据层 131

```bash
sudo mysql                                # root 走 auth_socket，无需口令
sudo redis-cli -a "$(sudo grep REDIS_PASSWORD /aitest/mysql/.credentials | cut -d= -f2)" --no-auth-warning
tail -f /aitest/mysql/logs/error.log
tail -f /aitest/mysql/logs/slow.log
sudo bash /aitest/scripts/backup-db.sh    # 手动触发备份
```

### 备份与恢复

自动备份：131 的 root crontab，每日 02:00，产物在 `/aitest/mysql/backup/`，保留 14 天。

```bash
# 恢复
gunzip < /aitest/mysql/backup/ai_marketplace_YYYYmmdd_HHMMSS.sql.gz | sudo mysql
```

> 作品文件在 **132 的 `/aitest/storage/files`**，当前不在自动备份范围内。
> 建议加一条从 132 到 131 的 rsync 定时任务，或后续把存储改为 NAS / 对象存储。

---

## 七、防火墙

| 机器 | 放行规则 |
|---|---|
| 131 | 22/tcp（任意）、3306/tcp（仅 192.168.1.132）、6379/tcp（仅 192.168.1.132） |
| 132 | 22/tcp、80/tcp、443/tcp、8081/tcp、8443/tcp（任意） |

两台默认策略均为 `deny (incoming), allow (outgoing)`，已设为开机启用。

> 若运营后台不应对外网段开放，把 132 的 8081 规则收窄到办公网段：
> `sudo ufw delete allow 8081/tcp && sudo ufw allow from <办公网段> to any port 8081 proto tcp`

---

## 八、从零重建（灾备参考）

两台都是全新 Ubuntu 24.04 时，完整流程：

```bash
# --- 两台都做 ---
sudo apt-get update
sudo mkdir -p /aitest/packages && sudo chown -R $USER /aitest
tar -xzf aitest-deploy.tar.gz -C /aitest/packages/deploy     # deploy/ 目录内容

# --- 131 数据层 ---
sudo apt-get install -y mysql-server redis-server curl net-tools rsync ufw
sudo bash /aitest/packages/deploy/scripts/setup-data-server.sh

# --- 132 应用层 ---
sudo apt-get install -y nginx openjdk-17-jre-headless curl net-tools rsync ufw
# 上传发布产物
sudo mkdir -p /aitest/app/releases /aitest/www/{workbench,admin}
sudo cp ai-marketplace.jar /aitest/app/releases/
sudo cp -r workbench-dist/. /aitest/www/workbench/
sudo cp -r admin-dist/.     /aitest/www/admin/
# 传递口令（不经中间落盘、不回显）
ssh aiuser@192.168.1.131 'sudo cat /aitest/mysql/.credentials' \
  | ssh aiuser@192.168.1.132 'umask 077; cat > /tmp/.aitest-cred'
sudo CRED_FILE=/tmp/.aitest-cred bash /aitest/packages/deploy/scripts/setup-app-server.sh
sudo shred -u /tmp/.aitest-cred
```

两个 setup 脚本都是**幂等**的，可重复执行；重复执行不会重新生成口令，
也不会覆盖已有的 `JWT_SECRET`（避免全员掉线）。

---

## 九、部署过程中踩到的坑（重建时务必注意）

1. **MySQL datadir 迁移会被 AppArmor 拦住**
   Ubuntu 的 `mysqld` 受 AppArmor 约束，默认只允许 `/var/lib/mysql`。
   解法是往 `/etc/apparmor.d/local/usr.sbin.mysqld` 写放行规则
   （主 profile 末尾有 `#include <local/...>`），而不是去改主 profile——后者会被包升级覆盖。

2. **`/etc/mysql/mysql.conf.d/zzz-aitest.cnf` 必须对 `mysql` 用户可读**
   systemd 是先切到 `mysql` 用户再 exec `mysqld` 的，而 MySQL 的 `!includedir`
   遇到不可读文件会在日志里报 `Permission denied` 后**停止处理整个 includedir**，
   服务却能正常启动——表现为"配置写了但完全不生效"，极易误判。
   脚本里已加断言：以 `mysql` 用户身份跑一次 `mysqld --print-defaults` 校验。

3. **Redis 的 systemd unit 带 `ProtectSystem=strict`**
   整个文件系统只读，`ReadWritePaths` 仅放行 `/var/lib/redis` 等。
   要用 `/aitest` 必须加 drop-in 追加 `ReadWritePaths=-/aitest/redis` 并覆盖 `ExecStart`。

4. **`umask 077` 会泄漏**
   在脚本里为生成口令文件设了 `umask 077` 后，后续所有 `cat >` 创建的文件都会变成 600，
   直接导致上面第 2 条的问题。已改为放进子 shell 隔离。

5. **132 出厂时 DNS 未配置**
   `ens160` 的 `ipv4.dns` 为空，路由与 TCP 出网其实正常，纯粹是域名解析不了。
   已用 `nmcli connection modify netplan-ens160 ipv4.dns 114.114.114.114` +
   `nmcli device reapply ens160` 修复（reapply 不会中断 SSH）。

6. **Nginx `client_max_body_size` 必须放大**
   后端 multipart 上限是 500MB，Nginx 默认 1m，不配会直接 413。两个站点都已配 500m，
   并加了 `proxy_request_buffering off` 让大文件不在 Nginx 落盘缓冲。

---

## 十、当前状态与下一步

**已验收通过：**

- 后端进程正常启动（约 5～7s），`systemctl` 已设开机自启
- `ai_market@192.168.1.132` 可连 MySQL，16 张表与分类种子数据完整
- Redis 口令认证与读写正常，持久化目录已迁至 `/aitest/redis/data`
- **登录链路已打通并浏览器实操验证**：运营后台登录后跳 `/dashboard`、
  工作台登录后跳首页、个人中心 `/me` 正确显示「信息技术部 · ADMIN / OPERATOR / USER」
- 接口层实测：错误口令与不存在账号均返回 1004（不泄露账号存在性）、
  无 token 调 `/auth/me` 返回 1001、`/api/submissions` 等分页接口返回正确 JSON
- HTTPS 已启用（443 工作台 / 8443 后台），证书链完整，Linux 标准 CA 库校验 `Verify return code: 0 (ok)`
- 企微凭据已写入，服务器实测 `gettoken` 返回 `errcode 0`
- 每日备份已试跑成功

**待办一：企微 SSO 启用（网络与企微后台侧，代码已就绪）**

后端 `/api/auth/wecom/redirect` 已能返回正确拼装的授权地址，但 `redirect_uri`
现在还是内网 IP，企微会拒绝。需要：

1. 定域名（通配符证书已覆盖，建议 `ai.tri-ibiotech.com`）并加 DNS 解析
2. 让公网入口把该域名 443 回源到 192.168.1.132
   （注意 `www.tri-ibiotech.com` 当前走第三方 WAF `ipm0ddpz.waf.zfuwuqi.com`，
   而 `121.46.250.190:80/443` 上是另一个 nginx，都不是本机）
3. 企微后台配置**可信域名**并做归属验证——验证文件已就位，
   实测 `https://<域名>/WW_verify_61IR393ntYc1jRjW.txt` 返回正确内容
4. 把 env 里的 `WECOM_AUTH_CALLBACK` 改为 `https://<正式域名>/login`
5. 确认「企业可信IP」含 `121.46.250.190`（gettoken 已成功，说明当前是放行的）
6. 全部就位后，把 `AUTH_LOCAL_LOGIN_ENABLED` 改回 `false`

**待办二：运营后台接口补齐（代码侧）**

后台现在能登录、能进去，但除「大赛配置」外每个菜单都会报「服务端异常」，
因为对应 Controller 尚未实现。完整清单、实测证据与修复建议见
[`docs/KNOWN-GAPS.md`](docs/KNOWN-GAPS.md)。
