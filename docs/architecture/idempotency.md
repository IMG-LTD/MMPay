# Idempotency

Payment creation, provider callbacks, refunds, and outbound webhooks will use
stable idempotency keys. Duplicate events must be detected explicitly and must
not mutate state after the first accepted event.
