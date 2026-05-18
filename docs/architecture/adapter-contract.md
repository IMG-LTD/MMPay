# Adapter Contract

Provider adapters will expose payment creation, payment query, refund, inbound
webhook verification, runtime status, invoice support status, and capability
discovery.

Adapters must surface provider failures explicitly and must not return mock
success results.

Runtime status is separate from capability discovery. A provider may declare the
operations it is designed to support while reporting `available=false` until its
live client, credentials, and provider review are wired. Callers must treat an
unavailable runtime status as a blocking condition, not as permission to fall
back to fake success.

Invoice support must be reported through the adapter SPI. A provider with no
approved invoice integration must return `unsupported by current provider`
instead of fabricating invoice identifiers or marking invoice delivery as
available.

Provider-specific request preparation may exist below the adapter SPI before a
live HTTP client is connected. That preparation may sign provider payloads,
construct provider headers, prepare query and refund envelopes, verify inbound
provider callbacks, and build provider-required callback acknowledgements. It
must not mark runtime status available or return successful payment results
until a real provider endpoint has been wired and evidenced.
