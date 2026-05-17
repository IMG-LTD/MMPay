# Backup And Restore

The minimal profile uses PostgreSQL. Back up the database before replacing the
`mmpay-app` image or running migration-bearing changes.

## Backup

```bash
pg_dump "$MMPAY_DATABASE_URL" --format=custom --file=mmpay-backup.dump
```

For the local compose profile, run the command from a host or container that can
reach the `postgres` service and provide credentials through environment
variables.

## Restore

```bash
pg_restore --clean --if-exists --dbname="$MMPAY_DATABASE_URL" mmpay-backup.dump
```

Restores must be tested in an isolated environment before touching a shared or
production database.
