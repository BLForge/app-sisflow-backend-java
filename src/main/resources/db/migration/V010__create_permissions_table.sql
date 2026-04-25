CREATE TABLE IF NOT EXISTS permissions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  code VARCHAR(100) NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL,
  category VARCHAR(50) NOT NULL,
  description TEXT,
  is_system BOOLEAN DEFAULT false,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_permissions_code ON permissions(code);
CREATE INDEX idx_permissions_category ON permissions(category);

-- Insert default permissions
INSERT INTO permissions (code, name, category, is_system, description) VALUES
-- TICKET
('ticket:create', 'Create Ticket', 'TICKET', true, 'Create new tickets'),
('ticket:read', 'Read Ticket', 'TICKET', true, 'Read tickets'),
('ticket:update', 'Update Ticket', 'TICKET', true, 'Update ticket details'),
('ticket:delete', 'Delete Ticket', 'TICKET', true, 'Delete tickets'),
('ticket:moderate', 'Moderate Ticket', 'TICKET', true, 'Moderate tickets'),
('ticket:assign', 'Assign Ticket', 'TICKET', true, 'Assign tickets to users'),
-- SETUP
('ticket:type:create', 'Create Ticket Type', 'SETUP', true, 'Create ticket types'),
('ticket:type:update', 'Update Ticket Type', 'SETUP', true, 'Update ticket types'),
('ticket:status:create', 'Create Ticket Status', 'SETUP', true, 'Create ticket statuses'),
('ticket:status:update', 'Update Ticket Status', 'SETUP', true, 'Update ticket statuses'),
('ticket:priority:create', 'Create Ticket Priority', 'SETUP', true, 'Create priorities'),
('ticket:priority:update', 'Update Ticket Priority', 'SETUP', true, 'Update priorities'),
('sla:create', 'Create SLA', 'SETUP', true, 'Create SLAs'),
('sla:update', 'Update SLA', 'SETUP', true, 'Update SLAs'),
('project:create', 'Create Project', 'SETUP', true, 'Create projects'),
('project:update', 'Update Project', 'SETUP', true, 'Update projects'),
('project:delete', 'Delete Project', 'SETUP', true, 'Delete projects'),
-- INTEGRATION
('integration:read', 'Read Integration', 'INTEGRATION', true, 'Read integrations'),
('integration:execute', 'Execute Integration', 'INTEGRATION', true, 'Execute integrations'),
('integration:delete', 'Delete Integration', 'INTEGRATION', true, 'Delete integrations'),
('integration:create', 'Create Integration', 'INTEGRATION', true, 'Create integrations'),
('integration:update', 'Update Integration', 'INTEGRATION', true, 'Update integrations'),
-- USER
('user:create', 'Create User', 'USER', true, 'Create users'),
('user:read', 'Read User', 'USER', true, 'Read user data'),
('user:update', 'Update User', 'USER', true, 'Update users'),
('user:delete', 'Delete User', 'USER', true, 'Delete users'),
-- ROLE
('role:create', 'Create Role', 'ROLE', true, 'Create roles'),
('role:read', 'Read Role', 'ROLE', true, 'Read roles'),
('role:update', 'Update Role', 'ROLE', true, 'Update roles'),
('role:delete', 'Delete Role', 'ROLE', true, 'Delete roles'),
-- SYSTEM
('system:read', 'Read System Config', 'SYSTEM', true, 'Read system configuration'),
('system:update', 'Update System Config', 'SYSTEM', true, 'Update system configuration')
ON CONFLICT (code) DO NOTHING;
