# MMPay Deprecation Policy

The v1.x line is patch-only. New feature scope resumes in v1.1.0 and
later minor releases.

## License Relay

`/api/license-relay/v1/forward` is stable across v1.x. A future v2 relay
protocol must ship as a parallel endpoint with at least a one-year overlap
before v1 can be removed.

## Schema

A column or table must remain deprecated for at least one minor release
before removal. Schema elements first marked deprecated in v1.0.0 cannot
be dropped before v1.2.0.

## Provider Adapters

No provider adapter may enter deprecation while it is the sole live
adapter. Huifu is the sole live adapter for v1.0.0, so it cannot be
deprecated in the v1.x line unless another provider reaches equivalent
GA maturity first.
