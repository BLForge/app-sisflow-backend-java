ALTER TABLE tickets 
ADD COLUMN IF NOT EXISTS visibility VARCHAR(50) DEFAULT 'PROJECT';

CREATE INDEX idx_tickets_visibility ON tickets(visibility);
