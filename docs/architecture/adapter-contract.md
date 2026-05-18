# Adapter Contract

Provider adapters will expose payment creation, payment query, refund, inbound
webhook verification, invoice support status, and capability discovery.

Adapters must surface provider failures explicitly and must not return mock
success results.

Invoice support must be reported through the adapter SPI. A provider with no
approved invoice integration must return `unsupported by current provider`
instead of fabricating invoice identifiers or marking invoice delivery as
available.
