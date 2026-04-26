# Button Backup

Daily PostgreSQL backups with a 7/4/6 retention policy (daily/weekly/monthly).

## Files

| File | Description |
|------|-------------|
| `backup.sh` | Dumps the DB and manages retention cleanup |
| `restore.sh` | Restores the DB from a dump file |
| `button-backup.service` | Systemd oneshot service that runs `backup.sh` |
| `button-backup.timer` | Systemd timer that fires nightly at 02:00 |

## How backups work

`backup.sh` runs `pg_dump --format=custom` and gzips the output to `~/backups/daily/button_YYYY-MM-DD.dump.gz`. It then conditionally copies that file to:

- `~/backups/weekly/` — if today is Sunday
- `~/backups/monthly/` — if today is the 1st of the month

After copying, it prunes old files in each directory, keeping only the newest N:

- **daily**: 7 files
- **weekly**: 4 files
- **monthly**: 6 files

The DB name defaults to `button` but is read from the `$DB_NAME` environment variable if set (the service unit loads `/home/button/button.env` which sets this in production).

## Installation

Because `button` is not a sudoer, installation is split: `button` owns everything under `~/backup/`, but a sudoer must install the systemd units.

### 1. Deploy the scripts (as `button`)

```bash
mkdir -p ~/backup ~/backups
cp backup.sh restore.sh ~/backup/
chmod +x ~/backup/backup.sh ~/backup/restore.sh
```

### 2. Install the systemd units (as a sudoer)

```bash
sudo cp button-backup.service button-backup.timer /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now button-backup.timer
```

Verify the timer is scheduled:

```bash
systemctl list-timers button-backup.timer
```

### 3. Verify with a manual run (as `button`)

```bash
systemctl start button-backup.service   # requires sudo, or ask a sudoer
# or run the script directly:
~/backup/backup.sh
ls ~/backups/daily/
```

## Restoring

Pass any `.dump.gz` file from any of the three backup directories:

```bash
~/backup/restore.sh ~/backups/daily/button_2026-04-26.dump.gz
```

The script drops all existing objects in the DB and restores from the dump. It does **not** drop or recreate the database itself, so no superuser privileges are needed.

> **Note:** `--no-owner` is passed to `pg_restore`, so restored objects will be owned by whichever role runs the restore. In production this is `button`, which is correct.
