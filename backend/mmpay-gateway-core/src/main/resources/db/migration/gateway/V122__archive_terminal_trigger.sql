CREATE OR REPLACE FUNCTION reject_archive_resurrection()
RETURNS TRIGGER AS $$
BEGIN
  IF OLD.status = 'archived' AND NEW.status <> 'archived' THEN
    RAISE EXCEPTION 'archived row is terminal: id=%', OLD.id
      USING ERRCODE = '23514';
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER merchants_reject_archive_resurrection
BEFORE UPDATE ON merchants
FOR EACH ROW
EXECUTE FUNCTION reject_archive_resurrection();

CREATE TRIGGER channels_reject_archive_resurrection
BEFORE UPDATE ON channels
FOR EACH ROW
EXECUTE FUNCTION reject_archive_resurrection();
