-- Ensure visibility column exists on ticket_interactions
-- This handles the case where V016 was marked as executed but never actually ran
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'ticket_interactions' AND column_name = 'visibility'
    ) THEN
        ALTER TABLE ticket_interactions 
        ADD COLUMN visibility VARCHAR(50) DEFAULT 'PROJECT' NOT NULL;
        
        CREATE INDEX idx_ticket_interactions_visibility ON ticket_interactions(visibility);
    END IF;
END $$;
