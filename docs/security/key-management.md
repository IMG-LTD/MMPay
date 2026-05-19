# Key Management

MMPay source control may contain only placeholders such as `replace-with-*`.

Runtime secrets must be injected through environment variables, secret files, or
external KMS references controlled by the operator.

Huifu sandbox RSA private keys, webhook endpoint keys, merchant identifiers, and
SDK paths are runtime configuration. They must be provided through external
Secret or environment injection and must not be committed to source control.

The vendor binding public key may be committed as `docs/security/public-key.asc`
and pinned from `governance/vendor-keys.yaml`. Local PGP private keys and
revocation certificates must stay outside Git; `.gitignore` keeps
`docs/security/private-key.asc` and `docs/security/revocation-certificate.asc`
local-only if an operator temporarily places them beside the public key.
