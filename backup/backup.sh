#!/usr/bin/env bash
set -euo pipefail

DB_NAME="${DB_NAME:-button}"
BACKUP_ROOT="${HOME}/backups"
DATE=$(date +%Y-%m-%d)
DOW=$(date +%u)   # 1=Mon … 7=Sun
DOM=$(date +%d)   # day of month, zero-padded

DAILY_DIR="${BACKUP_ROOT}/daily"
WEEKLY_DIR="${BACKUP_ROOT}/weekly"
MONTHLY_DIR="${BACKUP_ROOT}/monthly"

mkdir -p "$DAILY_DIR" "$WEEKLY_DIR" "$MONTHLY_DIR"

DUMP_FILE="${DAILY_DIR}/button_${DATE}.dump.gz"

pg_dump --format=custom "$DB_NAME" | gzip > "$DUMP_FILE"

# Weekly: keep Sundays (DOW == 7)
if [ "$DOW" -eq 7 ]; then
    cp "$DUMP_FILE" "${WEEKLY_DIR}/button_${DATE}.dump.gz"
fi

# Monthly: keep 1st of month
if [ "$DOM" -eq "01" ]; then
    cp "$DUMP_FILE" "${MONTHLY_DIR}/button_${DATE}.dump.gz"
fi

# Retention cleanup
find "$DAILY_DIR"   -name "button_*.dump.gz" | sort | head -n -7  | xargs -r rm --
find "$WEEKLY_DIR"  -name "button_*.dump.gz" | sort | head -n -4  | xargs -r rm --
find "$MONTHLY_DIR" -name "button_*.dump.gz" | sort | head -n -6  | xargs -r rm --
