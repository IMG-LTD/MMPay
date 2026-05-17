# Threat Model

Primary risks include credential exposure, forged provider callbacks, replayed
webhooks, duplicate refunds, and accidental license signing capability.

The MP-0 guardrail is simple: no secrets and no license signing implementation
belong in the repository.
