# Upgrade

MMPay uses Flyway-style migration files under backend modules. Before upgrading
or cutting a release candidate, validate migration naming and the full local
gate:

```bash
bash scripts/check-migration-naming.sh
bash scripts/validate-local.sh
```

For local Docker upgrades, rebuild the app image from the target commit and
restart the minimal compose profile:

```bash
docker build -t mmpay-app:local .
docker compose -f deploy/docker-compose.minimal.yml up
```

Do not mark an upgrade as payment-complete until real provider evidence has been
captured and validated.
