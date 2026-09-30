# 企业 AI 应用市场 — 内网部署手册

部署完成时间：2026-09-21　｜　最近更新：2026-09-23（启用 HTTPS、登录链路打通）
状态：基础设施、应用进程、登录（账号口令）均已验收通过；企微 SSO 待可信域名配置，运营后台接口待补（见 `docs/KNOWN-GAPS.md`）

---

## 一、部署拓扑

```
   企业微信客户端 ─┐
   员工浏览器 ─────┼─► https://ai-marketplace.tri-ibiotech.com:8081/
                  │        │ DNS → 121.46.250.190（公网，已存在）
                  │        │ 防火墙 DNAT 8081 → 192.168.1.132:8081（已存在）
                  │        ▼
                  │   ┌──────────────────────────────────────────┐
   运营/超管 ─────┼──►│  App-AI-Test  192.168.1.132 (16G)         │
   （仅内网）      │   │  Ubuntu 24.04 · 应用层                    │
                  │   │                                          │
                  │   │  Nginx 1.24 · TLS: *.tri-ibiotech.com     │
                  │   │    :8081 HTTPS → 员工工作台（对外正式入口）│
                  │   │    :8443 HTTPS → 运营后台（ufw 限内网）    │
                  │   │    :443  HTTPS → 员工工作台（内网直连）    │
                  │   │    :80   HTTP  → 301 跳到 :8081 正式入口   │
                  │   │    /api/       → 127.0.0.1:8080           │
                  │   │  Spring Boot 3.3.4 (systemd) :8080        │
                  │   │    context-path=/api                      │
                  │   └─────────────────┬────────────────────────┘
                                        │ 3306 / 6379
                                        │ (ufw 仅放行 132)
                  ┌─────────────────────▼────────────────────────┐
                  │  Mysql-AI-Test 192.168.1.131 (8G)            │
                  │  Ubuntu 24.04 · 数据层                        │
                  │  MySQL 8.0.46   datadir=/aitest/mysql/data    │
                  │  Redis 7.0.15   dir=/aitest/redis/data        │
                  │  每日 02:00 自动备份，保留 14 天                │
                  └──────────────────────────────────────────────┘
```

> 同一台 132 上还跑着另一个不属于本项目的服务：`/aitest/tri-meeting/`（监听 127.0.0.1:8090，
> 由 Nginx 8082 反代）。本项目的脚本不会触碰它，改动 Nginx 时注意别覆盖 8082 的站点配置。

**为什么运营后台用独立端口而不是 README 里写的 `/admin` 子路径：**
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

| 用途 | 地址 | 可达范围 |
|---|---|---|
| **员工工作台（企微正式入口）** | `https://ai-marketplace.tri-ibiotech.com:8081/` | 公网 + 内网 |
| 运营后台 | `https://192.168.1.132:8443/` | **仅内网**（ufw 限 RFC1918） |
| 员工工作台（内网直连） | `https://192.168.1.132/`（443） | 仅内网 |
| HTTP 80 | `http://192.168.1.132/` | 301 跳转到正式入口 |
| 后端（内部） | `http://192.168.1.132:8080/api` | 仅本机，ufw 未放行 |
| 接口文档 | 生产 profile 下已被 `knife4j.production=true` 关闭 | — |

实测确认：正式入口的证书链与主机名校验均通过
（Linux 侧 `curl` 不加 `-k` 返回 `ssl_verify_result=0`，
`openssl s_client -verify_hostname ai-marketplace.tri-ibiotech.com` 返回 `Verification: OK`）。

**运营后台用 IP 访问会有「证书名称不匹配」告警**（证书是 `*.tri-ibiotech.com`，不含 IP），
需手动点继续。要消除告警，推荐给内网 DNS 加一条 split-horizon 记录：
把 `ai-marketplace.tri-ibiotech.com` 在**内网**解析到 `192.168.1.132`。
这样内网用户走 `https://ai-marketplace.tri-ibiotech.com:8443/`（证书匹配、无告警），
外网企微客户端仍解析到公网 IP 走 8081，两边都对，还顺带省掉 NAT 回环。

公网端口映射实测现状（`121.46.250.190`）：

| 端口 | 指向 | 说明 |
|---|---|---|
| 8081 | **本项目 132:8081** | 已存在，本次直接复用，无需网络侧改动 |
| 80 / 443 | 公司官网「源资科技」nginx | 不是本项目；企微域名验证文件恰好也由它提供（内容一致） |
| 8443 | 其它服务 | 不是本项目，因此运营后台无法经公网域名访问 |

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
| 132 | 22/tcp、80/tcp、443/tcp、8081/tcp（任意）；**8443/tcp 仅限 10.0.0.0/8、172.16.0.0/12、192.168.0.0/16** |

两台默认策略均为 `deny (incoming), allow (outgoing)`，已设为开机启用。

> **8081 必须保持对 Anywhere 放行** —— 它是企业微信侧的正式入口，
> 员工手机在 4G/5G 下要能直连，收窄了企微里就打不开。
>
> **8443 不要再放开到 Anywhere** —— 那是运营后台，且当前开着账号口令登录，
> 公网明文/可达都会让口令面临风险。若要临时给外部运营人员开权限，
> 用 `sudo ufw allow from <具体IP> to any port 8443 proto tcp` 精确放行，用完删掉。
>
> 若日后 80 端口也要对外，注意它现在是 301 跳转到 `https://<域名>:8081`，
> 不是直接提供业务。

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
- **登录链路已打通并浏览器实操验证**：在正式入口
  `https://ai-marketplace.tri-ibiotech.com:8081/login` 用账号口令登录成功、跳转作品列表页；
  运营后台登录后跳 `/dashboard`；个人中心 `/me` 正确显示「信息技术部 · ADMIN / OPERATOR / USER」
- 接口层实测：错误口令与不存在账号均返回 1004（不泄露账号存在性）、
  无 token 调 `/auth/me` 返回 1001、`/api/submissions` 等分页接口返回正确 JSON
- HTTPS 已启用：8081（工作台，对外正式入口）、8443（后台，仅内网）、443（工作台，内网直连）；
  证书链完整，严格校验含主机名匹配均通过
- 企微凭据已写入，服务器实测 `gettoken` 返回 `errcode 0`
- 每日备份已试跑成功

**待办一：企微 SSO 启用（技术侧已全部就绪，只剩企微后台配置）**

已完成并实测通过的部分：

- DNS：`ai-marketplace.tri-ibiotech.com` → `121.46.250.190`（公网 DNS 已生效）
- 公网入口：`121.46.250.190:8081` → `192.168.1.132:8081` 的 DNAT 本来就存在，直接复用
- HTTPS：8081 已启用 TLS，证书 `*.tri-ibiotech.com` 覆盖该子域名，
  严格校验（含主机名）通过
- 回调地址：`WECOM_AUTH_CALLBACK=https://ai-marketplace.tri-ibiotech.com:8081/login`
- 域名归属验证文件：`https://ai-marketplace.tri-ibiotech.com/WW_verify_61IR393ntYc1jRjW.txt`
  实测返回 200 且内容为 `61IR393ntYc1jRjW`（由官网 nginx 提供，企微只要求域名根路径 443 可取）
- 授权地址拼装：实测 `/api/auth/wecom/redirect` 返回

  ```
  https://open.weixin.qq.com/connect/oauth2/authorize
    ?appid=wwca6dedc33172f790
    &redirect_uri=https%3A%2F%2Fai-marketplace.tri-ibiotech.com%3A8081%2Flogin
    &response_type=code&scope=snsapi_base&agentid=1000017#wechat_redirect
  ```

  在桌面浏览器点击「企微 SSO 登录」会跳到企微并显示「请在企业微信客户端打开链接」，
  这是 `snsapi_base` 授权的**预期行为**，且企微未报 `redirect_uri` 参数错误。

还需要在企微管理后台做的（需管理员权限）：

1. 「应用管理」→ 该自建应用 →「网页授权及JS-SDK」→ 设置**可信域名**
   为 `ai-marketplace.tri-ibiotech.com` 并完成归属验证（验证文件已就位）
2. 「工作台」应用主页 URL 填 `https://ai-marketplace.tri-ibiotech.com:8081/`
3. 确认「企业可信IP」含 `121.46.250.190`（`gettoken` 实测已返回 errcode 0，说明当前已放行）
4. 全部就位并在企微客户端内验证登录后，把 `AUTH_LOCAL_LOGIN_ENABLED` 改回 `false`

> **需要留意的风险**：`redirect_uri` 带了非标准端口 `:8081`。企微可信域名是按域名配置的，
> 大多数情况下允许带端口，但这一步只能在企微客户端内实测确认。
> 若企微拒绝带端口的回调，退路是让网络侧把公网 443 也映射到 132（或改用官网 nginx 反代），
> 然后把回调地址改成不带端口的 `https://ai-marketplace.tri-ibiotech.com/login`。

**待办二：运营后台接口补齐（代码侧）**

后台现在能登录、能进去，但除「大赛配置」外每个菜单都会报「服务端异常」，
因为对应 Controller 尚未实现。完整清单、实测证据与修复建议见
[`docs/KNOWN-GAPS.md`](docs/KNOWN-GAPS.md)。
