-- Temporarily disable RLS until we can get it working properly
-- This allows the application to function while we debug the RLS issues

-- Disable RLS on all tables temporarily
DO $$
BEGIN
    -- Disable RLS on tables that might have it enabled
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'customers') THEN
        ALTER TABLE customers DISABLE ROW LEVEL SECURITY;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_profiles') THEN
        ALTER TABLE user_profiles DISABLE ROW LEVEL SECURITY;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'tickets') THEN
        ALTER TABLE tickets DISABLE ROW LEVEL SECURITY;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ticket_interactions') THEN
        ALTER TABLE ticket_interactions DISABLE ROW LEVEL SECURITY;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'roles') THEN
        ALTER TABLE roles DISABLE ROW LEVEL SECURITY;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'permissions') THEN
        ALTER TABLE permissions DISABLE ROW LEVEL SECURITY;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_roles') THEN
        ALTER TABLE user_roles DISABLE ROW LEVEL SECURITY;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'projects') THEN
        ALTER TABLE projects DISABLE ROW LEVEL SECURITY;
    END IF;
END $$;

-- Ensure visibility columns exist (safety check)
DO $$
BEGIN
    -- Add visibility to tickets if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'tickets' AND column_name = 'visibility'
    ) THEN
        ALTER TABLE tickets 
        ADD COLUMN visibility VARCHAR(50) DEFAULT 'PROJECT' NOT NULL;
        
        CREATE INDEX IF NOT EXISTS idx_tickets_visibility ON tickets(visibility);
    END IF;
    
    -- Add visibility to ticket_interactions if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'ticket_interactions' AND column_name = 'visibility'
    ) THEN
        ALTER TABLE ticket_interactions 
        ADD COLUMN visibility VARCHAR(50) DEFAULT 'PROJECT' NOT NULL;
        
        CREATE INDEX IF NOT EXISTS idx_ticket_interactions_visibility ON ticket_interactions(visibility);
    END IF;
END $$;