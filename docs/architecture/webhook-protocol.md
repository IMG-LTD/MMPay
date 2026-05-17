# Webhook Protocol

MMPay outbound webhooks to MMMail will follow the MMMail billing webhook
signature contract.

Webhook payloads carry payment facts only. They must not contain license claim
fields such as `edition`, `seats`, `features`, `issuedAt`, or `expiresAt`.
