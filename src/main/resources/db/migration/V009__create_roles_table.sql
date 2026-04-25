-- Roles table is now created in V000__create_base_tables.sql
-- This migration ensures default roles are inserted

-- Insert default roles (if not already present)
INSERT INTO roles (id, code, name, hierarchy_level, is_system, description) VALUES
(gen_random_uuid(), 'client', 'Client', 0, true, 'End user / Customer'),
(gen_random_uuid(), 'developer', 'Developer', 1, true, 'Support Agent / Developer'),
(gen_random_uuid(), 'moderator', 'Moderator', 2, true, 'Content Moderator'),
(gen_random_uuid(), 'tenant_admin', 'Tenant Admin', 3, true, 'Tenant Administrator'),
(gen_random_uuid(), 'system_admin', 'System Admin', 4, true, 'System Administrator')
ON CONFLICT (code) DO NOTHING;
