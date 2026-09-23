#!/usr/bin/env bash
# =============================================================================
# 发版脚本 — 在应用服务器 132 上执行
# 把新的 jar / 前端产物替换到位并重启，保留上一版以便快速回滚。
#
# 用法：
#   bash /aitest/scripts/deploy-release.sh /tmp/release-YYYYmmdd.tar.gz
# 发布包内目录结构（由 build-release.ps1 生成）：
#   backend/ai-marketplace.jar
#   frontend-workbench/dist/...
#   frontend-admin/dist/...
# =============================================================================
set -euo pipefail

BASE=/aitest
PKG="${1:-}"
STAMP=$(date +%Y%m%d_%H%M%S)

[[ -n $PKG && -f $PKG ]] || { echo "用法: $0 <release.tar.gz>"; exit 1; }
[[ $EUID -eq 0 ]] || { echo "请用 sudo 运行"; exit 1; }

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
tar -xzf "$PKG" -C "$TMP"

echo "==> 备份当前版本"
[[ -f $BASE/app/releases/ai-marketplace.jar ]] && \
    cp -f "$BASE/app/releases/ai-marketplace.jar" "$BASE/app/releases/ai-marketplace.jar.prev-$STAMP"
# 只保留最近 3 个历史版本
ls -1t "$BASE"/app/releases/ai-marketplace.jar.prev-* 2>/dev/null | tail -n +4 | xargs -r rm -f

echo "==> 替换后端 jar"
install -m 0644 -o aiuser -g aiuser "$TMP/backend/ai-marketplace.jar" \
        "$BASE/app/releases/ai-marketplace.jar"

echo "==> 替换前端产物"
for pair in "frontend-workbench/dist:www/workbench" "frontend-admin/dist:www/admin"; do
    src="${pair%%:*}"; dst="$BASE/${pair##*:}"
    if [[ -d "$TMP/$src" ]]; then
        # 清空 www 会连带删掉企微域名归属验证文件 WW_verify_*.txt，
        # 之后企微「可信域名」会因取不到校验文件而静默失效，所以先挪出去再放回
        saved=$(mktemp -d)
        find "$dst" -maxdepth 1 -name 'WW_verify_*.txt' -exec cp {} "$saved"/ \; 2>/dev/null || true
        rm -rf "${dst:?}"/*
        cp -r "$TMP/$src"/. "$dst"/
        cp "$saved"/WW_verify_*.txt "$dst"/ 2>/dev/null || true
        rm -rf "$saved"
        chmod -R a+rX "$dst"
        echo "    $dst 已更新（企微验证文件已保留 $(ls "$dst"/WW_verify_*.txt 2>/dev/null | wc -l) 个）"
    else
        echo "    发布包中没有 $src，跳过"
    fi
done

echo "==> 重启后端"
systemctl restart ai-marketplace
for i in $(seq 1 60); do
    systemctl is-active --quiet ai-marketplace && \
        ss -ltn "sport = :8080" | grep -q 8080 && { echo "    已启动（${i}s）"; break; }
    sleep 1
    [[ $i -eq 60 ]] && { journalctl -u ai-marketplace -n 40 --no-pager; echo "启动超时，可回滚："; \
        echo "  cp $BASE/app/releases/ai-marketplace.jar.prev-$STAMP $BASE/app/releases/ai-marketplace.jar && systemctl restart ai-marketplace"; exit 1; }
done

# 前端是静态文件，reload 即可让 Nginx 重新打开文件句柄
nginx -t && systemctl reload nginx

echo "==> 完成。回滚命令（如需）："
echo "  sudo cp $BASE/app/releases/ai-marketplace.jar.prev-$STAMP $BASE/app/releases/ai-marketplace.jar && sudo systemctl restart ai-marketplace"
