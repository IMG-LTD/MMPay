# Restore Attestation Key Escrow

`MMPAY_AUDIT_RESTORE_ATTESTATION_KEY` is split into three operator
escrow shards. Any two shards are required to recover the key.

Loss of more than one shard is treated as a compromise-level event:

1. Freeze restore operations.
2. Rotate the restore attestation key.
3. Emit `system.attestation_key_rotated` under the old key and an
   operator PGP counter-signature.
4. Reseal three new shards and store them in separate locations.
