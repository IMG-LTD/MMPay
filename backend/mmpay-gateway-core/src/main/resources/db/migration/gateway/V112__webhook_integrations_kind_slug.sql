ALTER TABLE webhook_integrations
  ADD COLUMN IF NOT EXISTS kind VARCHAR(16);

ALTER TABLE webhook_integrations
  ADD COLUMN IF NOT EXISTS slug VARCHAR(64);

ALTER TABLE webhook_integrations
  ADD COLUMN IF NOT EXISTS relay_target_id VARCHAR(64);

UPDATE webhook_integrations
SET kind = COALESCE(kind, 'webhook'),
    slug = COALESCE(slug, lower(regexp_replace(id, '[^a-zA-Z0-9]+', '-', 'g')))
WHERE kind IS NULL OR slug IS NULL;

ALTER TABLE webhook_integrations
  ALTER COLUMN kind SET DEFAULT 'webhook',
  ALTER COLUMN kind SET NOT NULL,
  ALTER COLUMN slug SET NOT NULL;

ALTER TABLE webhook_integrations
  DROP CONSTRAINT IF EXISTS ck_webhook_integrations_kind;

ALTER TABLE webhook_integrations
  ADD CONSTRAINT ck_webhook_integrations_kind CHECK (kind IN ('webhook','relay'));

ALTER TABLE webhook_integrations
  DROP CONSTRAINT IF EXISTS ck_webhook_integrations_slug;

ALTER TABLE webhook_integrations
  ADD CONSTRAINT ck_webhook_integrations_slug CHECK (slug ~ '^[a-z][a-z0-9-]{2,63}$');

CREATE UNIQUE INDEX IF NOT EXISTS ux_webhook_integrations_tenant_slug
  ON webhook_integrations (tenant_id, slug);

ALTER TABLE webhook_integrations
  DROP CONSTRAINT IF EXISTS fk_webhook_integrations_relay_target;

ALTER TABLE webhook_integrations
  ADD CONSTRAINT fk_webhook_integrations_relay_target
    FOREIGN KEY (relay_target_id) REFERENCES license_relay_targets (id);
