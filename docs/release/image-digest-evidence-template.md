---
tag: v1.0.0-rc.N
commit_sha: 0123456789abcdef0123456789abcdef01234567
workflow_run_url: https://github.com/IMG-LTD/MMPay/actions/runs/0
registry_host: ghcr.io
mmpay-app: sha256:0000000000000000000000000000000000000000000000000000000000000000
mmpay-frontend-admin: sha256:0000000000000000000000000000000000000000000000000000000000000000
mmpay-app-debug-symbols: sha256:0000000000000000000000000000000000000000000000000000000000000000
compose_digest: sha256:0000000000000000000000000000000000000000000000000000000000000000
build_attestation_type: cosign
build_attestation_hash: sha256:0000000000000000000000000000000000000000000000000000000000000000
signing_key_fingerprint: SHA256:example
signed_by: github-actions
builder_identity: https://github.com/IMG-LTD/MMPay/.github/workflows/images.yml
registry_immutability_proof: github-package-tag-immutable-policy
---

# Image Digest Evidence

Evidence status: draft

Copy this template to `docs/release/v1.0.0-image-digest-evidence.md`
when preparing a v1 RC or GA promotion. Replace every digest and proof
with external evidence from the actual workflow run and registry.
