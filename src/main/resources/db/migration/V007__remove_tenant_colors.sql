-- Remove tenant color columns
ALTER TABLE tenants DROP COLUMN IF EXISTS primary_color;
ALTER TABLE tenants DROP COLUMN IF EXISTS secondary_color;
ALTER TABLE tenants DROP COLUMN IF EXISTS accent_color;
