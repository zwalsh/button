#!/usr/bin/env bash
set -euo pipefail

DB_NAME="${DB_NAME:-button}"

if [ $# -ne 1 ]; then
    echo "Usage: $0 <backup_file.dump.gz>" >&2
    exit 1
fi

BACKUP_FILE="$1"

if [ ! -f "$BACKUP_FILE" ]; then
    echo "Error: file not found: $BACKUP_FILE" >&2
    exit 1
fi

echo "Restoring $DB_NAME from $BACKUP_FILE ..."

# Drop all objects in the DB then restore — avoids needing to drop/recreate the DB itself.
gunzip -c "$BACKUP_FILE" | pg_restore --dbname="$DB_NAME" --clean --if-exists --no-owner --no-privileges

echo "Done."
