# Data Model

The core domain includes merchants, channels, payment intents, transactions,
refunds, invoice support status, and webhook events.

`Merchant` and `Channel` records store credential handles only. They do not
store provider credential plaintext, private keys, certificates, or webhook
secrets. A channel is bound to one merchant and one provider code.

`PaymentIntent` stores the upstream order reference and a required idempotency
key. The database migration keeps that idempotency key unique so duplicate
create requests can be rejected or mapped explicitly instead of creating a
second intent silently.

License claims are not part of the MMPay data model.
