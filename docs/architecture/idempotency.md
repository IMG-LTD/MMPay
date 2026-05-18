# Idempotency

Payment creation, provider callbacks, refunds, and outbound webhooks use stable
idempotency keys. `PaymentIntent` carries the upstream idempotency key, and the
schema keeps it unique. Duplicate events must be detected explicitly and must not
mutate state after the first accepted event.
