# Adapter Contract

Provider adapters will expose payment creation, payment query, refund, inbound
webhook verification, and capability discovery.

Adapters must surface provider failures explicitly and must not return mock
success results.
