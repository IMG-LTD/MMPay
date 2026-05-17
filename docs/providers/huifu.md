# Huifu Provider

Huifu is the first planned provider adapter. Sandbox credentials must be
provided through environment variables, secret files, or an external secret
manager.

No Huifu merchant ID, private key, certificate, or sandbox token may be stored
in this repository.

## dg-payment-skills Boundary

`huifurepo/dg-payment-skills` is review-only guidance for Huifu integration
research. Its repository declares CC BY-NC 4.0 and Huifu-owned technical
content, so its contents must not be compiled into MMPay, vendored, copied into
source, or redistributed in release artifacts.

The current reviewed facts are:

- Java SDK baseline documented by the skill package: `dg-java-sdk 3.0.36`.
- Runtime credentials remain outside source control.
- Provider failures must surface explicitly and must not become mock success.

MMPay may use this guidance to shape adapter contracts, but production Huifu
HTTP or SDK wiring requires a separate license and merchant credential review
before adding any runtime dependency.
