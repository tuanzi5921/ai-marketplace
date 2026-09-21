#!/usr/bin/env bash
# =============================================================================
# 数据层每日备份 — 建议由 root 的 crontab 调用
#   0 2 * * * /aitest/scripts/backup-db.sh >> /aitest/mysql/logs/backup.log 2>&1
#
# 产物：/aitest/mysql/backup/ai_marketplace_YYYYmmdd_HHMMSS.sql.gz
#       /aitest/mysql/backup/redis_dump_YYYYmmdd_HHMMSS.rdb.gz
# 保留最近 KEEP_DAYS 天。
# =============================================================================
set -euo pipefail

BASE=/aitest
KEEP_DAYS="${KEEP_DAYS:-14}"
STAMP=$(date +%Y%m%d_%H%M%S)
OUT="$BASE/mysql/backup"
DB_NAME=ai_marketplace
CRED_FILE="$BASE/mysql/.credentials"

mkdir -p "$OUT"
[[ -f $CRED_FILE ]] && source "$CRED_FILE"

echo "[$(date '+%F %T')] 开始备份"

# --single-transaction 保证 InnoDB 一致性快照且不锁表
mysqldump --single-transaction --quick --routines --triggers --events \
          --default-character-set=utf8mb4 \
          --databases "$DB_NAME" | gzip -6 > "$OUT/ai_marketplace_$STAMP.sql.gz"
echo "  MySQL: $(du -h "$OUT/ai_marketplace_$STAMP.sql.gz" | cut -f1)"

# Redis 只缓存企微 access_token，丢了会自动重建，但仍顺带备一份
if [[ -n ${REDIS_PASSWORD:-} ]]; then
    redis-cli -a "$REDIS_PASSWORD" --no-auth-warning BGSAVE >/dev/null
    # 等待后台落盘完成
    while [[ $(redis-cli -a "$REDIS_PASSWORD" --no-auth-warning LASTSAVE) == "" ]]; do sleep 1; done
    sleep 2
    [[ -f $BASE/redis/data/dump.rdb ]] && \
        gzip -6c "$BASE/redis/data/dump.rdb" > "$OUT/redis_dump_$STAMP.rdb.gz" && \
        echo "  Redis: $(du -h "$OUT/redis_dump_$STAMP.rdb.gz" | cut -f1)"
fi

find "$OUT" -name '*.gz' -mtime +"$KEEP_DAYS" -print -delete
echo "[$(date '+%F %T')] 备份完成，保留 ${KEEP_DAYS} 天"
