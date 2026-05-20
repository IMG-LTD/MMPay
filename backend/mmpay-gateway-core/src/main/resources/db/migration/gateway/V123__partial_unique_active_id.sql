ALTER TABLE channels DROP CONSTRAINT fk_channels_merchant;
ALTER TABLE merchants ADD COLUMN row_uid UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE channels ADD COLUMN row_uid UUID NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE merchants DROP CONSTRAINT merchants_pkey;
ALTER TABLE channels DROP CONSTRAINT channels_pkey;

ALTER TABLE merchants ADD PRIMARY KEY (row_uid);
ALTER TABLE channels ADD PRIMARY KEY (row_uid);

CREATE UNIQUE INDEX merchants_id_active_uniq
  ON merchants (id)
  WHERE status <> 'archived';

CREATE UNIQUE INDEX channels_id_active_uniq
  ON channels (id)
  WHERE status <> 'archived';

CREATE INDEX channels_merchant_id_idx
  ON channels (merchant_id);
