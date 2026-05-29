-- Add color customization fields to tenants
ALTER TABLE tenants
ADD COLUMN primary_color VARCHAR(7),
ADD COLUMN secondary_color VARCHAR(7),
ADD COLUMN accent_color VARCHAR(7);

-- Set default colors (optional)
UPDATE tenants SET 
    primary_color = '#3b82f6',
    secondary_color = '#8b5cf6',
    accent_color = '#10b981'
WHERE primary_color IS NULL;
