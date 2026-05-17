# Architecture Overview

MMPay is an independent payment gateway repository for MMMail and other
self-hosted products. It uses a Pig-compatible Spring Cloud Alibaba backend
foundation and a soybean-admin style frontend built with Vue, Pinia, Vite, and
Naive UI.

## Backend Modules

- `mmpay-gateway-core`: payment intent, transaction, refund, reconciliation,
  and migration contracts.
- `mmpay-adapter-spi`: provider adapter boundary and explicit unavailable
  operation errors.
- `mmpay-adapter-huifu`: Huifu adapter boundary and reconciliation mapping.
- `mmpay-webhook-out`: signed MMMail webhook delivery and replay idempotency.
- `mmpay-license-relay`: opaque relay-only boundary; no license signing.
- `mmpay-admin-api`: read-only admin dashboard API.
- `mmpay-app`: Spring Boot entry point and Docker image target.

## Runtime Boundary

The repository can build and run a minimal local backend image, but real Huifu
sandbox payments, provider credentials, and vendor license issuance remain
external evidence requirements.
