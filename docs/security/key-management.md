# Key Management

MMPay source control may contain only placeholders such as `replace-with-*`.

Runtime secrets must be injected through environment variables, secret files, or
external KMS references controlled by the operator.

Huifu sandbox RSA private keys, webhook endpoint keys, merchant identifiers, and
SDK paths are runtime configuration. They must be provided through external
Secret or environment injection and must not be committed to source control.
