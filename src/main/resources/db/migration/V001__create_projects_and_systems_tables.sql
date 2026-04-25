-- Projects and systems tables are now created in V000__create_base_tables.sql
-- This migration is kept for reference but the tables are already created

-- Add project and system references to tickets table if not already present
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS project_id UUID;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS system_id UUID;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS github_pull_request_url VARCHAR(500);

-- Add constraints if they don't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_tickets_project'
    ) THEN
        ALTER TABLE tickets ADD CONSTRAINT fk_tickets_project FOREIGN KEY (project_id) REFERENCES projects(id);
    END IF;
    
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_tickets_system'
    ) THEN
        ALTER TABLE tickets ADD CONSTRAINT fk_tickets_system FOREIGN KEY (system_id) REFERENCES systems(id);
    END IF;
END $$;

-- Update github_configuration table
ALTER TABLE github_configuration ADD COLUMN IF NOT EXISTS project_id UUID;
ALTER TABLE github_configuration ADD COLUMN IF NOT EXISTS webhook_secret VARCHAR(255);
ALTER TABLE github_configuration ADD COLUMN IF NOT EXISTS enabled BOOLEAN DEFAULT true;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_github_config_project'
    ) THEN
        ALTER TABLE github_configuration ADD CONSTRAINT fk_github_config_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_tickets_project_id ON tickets(project_id);
CREATE INDEX IF NOT EXISTS idx_tickets_system_id ON tickets(system_id);
CREATE INDEX IF NOT EXISTS idx_github_config_project_id ON github_configuration(project_id);
