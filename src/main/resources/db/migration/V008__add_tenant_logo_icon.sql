-- Add logo icon URL for collapsed sidebar
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS logo_icon_url VARCHAR(500);
