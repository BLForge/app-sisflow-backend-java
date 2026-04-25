-- Fix missing visibility columns on both tickets and ticket_interactions
-- This handles the case where migrations were marked as executed but never actually ran

DO $$
BEGIN
    -- Add visibility to tickets if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'tickets' AND column_name = 'visibility'
    ) THEN
        ALTER TABLE tickets 
        ADD COLUMN visibility VARCHAR(50) DEFAULT 'PROJECT' NOT NULL;
        
        CREATE INDEX idx_tickets_visibility ON tickets(visibility);
    END IF;
    
    -- Add visibility to ticket_interactions if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'ticket_interactions' AND column_name = 'visibility'
    ) THEN
        ALTER TABLE ticket_interactions 
        ADD COLUMN visibility VARCHAR(50) DEFAULT 'PROJECT' NOT NULL;
        
        CREATE INDEX idx_ticket_interactions_visibility ON ticket_interactions(visibility);
    END IF;
END $$;
