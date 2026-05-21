# Image Publishing

MMPay publishes the runnable app, standalone admin frontend, and debug symbols
images through `.github/workflows/images.yml`.

The workflow runs on version tags and manual dispatch, builds the repository
root `Dockerfile`, and publishes:

- `ghcr.io/img-ltd/mmpay-app:<tag-or-ref>`
- `ghcr.io/img-ltd/mmpay-app:<commit-sha>`
- `ghcr.io/img-ltd/mmpay-frontend-admin:<tag-or-ref>`
- `ghcr.io/img-ltd/mmpay-frontend-admin:<commit-sha>`
- `ghcr.io/img-ltd/mmpay-app-debug-symbols:<tag-or-ref>`
- `ghcr.io/img-ltd/mmpay-app-debug-symbols:<commit-sha>`

The image contains the Spring Boot `mmpay-app` backend and the built
`frontend-admin` static assets. The root path `/` serves the admin UI, while
`/api/*` and `/actuator/*` remain backend routes. Merchant credentials, provider
keys, webhook secrets, and license signing keys must be provided at runtime
through environment variables, secret files, or an external secret manager.

The v1.0.0 published tags are:

```text
ghcr.io/img-ltd/mmpay-app:v1.0.0
ghcr.io/img-ltd/mmpay-frontend-admin:v1.0.0
ghcr.io/img-ltd/mmpay-app-debug-symbols:v1.0.0
```

For Docker quick-start validation, prefer the current source checkout or a
published `v1.0.1` patch image. The immutable `v1.0.0` app image predates the
`SPRING_DATASOURCE_*` Compose wiring and entrypoint guard documented in
`docs/release/v1.0.1-release-notes.md`.
