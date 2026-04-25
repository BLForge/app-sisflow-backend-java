ALTER TABLE ticket_interactions 
ADD COLUMN IF NOT EXISTS visibility VARCHAR(50) DEFAULT 'PROJECT';

CREATE INDEX idx_ticket_interactions_visibility ON ticket_interactions(visibility);
