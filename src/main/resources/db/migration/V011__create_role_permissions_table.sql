CREATE TABLE IF NOT EXISTS role_permissions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
  permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT now(),
  UNIQUE(role_id, permission_id)
);

CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);

-- Map CLIENT permissions
INSERT INTO role_permissions (role_id, permission_id, is_active)
SELECT r.id, p.id, true FROM roles r, permissions p
WHERE r.code = 'client' AND p.code IN ('ticket:create', 'ticket:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Map DEVELOPER permissions
INSERT INTO role_permissions (role_id, permission_id, is_active)
SELECT r.id, p.id, true FROM roles r, permissions p
WHERE r.code = 'developer' AND p.code IN (
  'ticket:create', 'ticket:read', 'ticket:update', 'ticket:delete',
  'ticket:moderate', 'ticket:assign',
  'integration:read', 'integration:execute', 'integration:delete'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Map MODERATOR permissions
INSERT INTO role_permissions (role_id, permission_id, is_active)
SELECT r.id, p.id, true FROM roles r, permissions p
WHERE r.code = 'moderator' AND p.code IN (
  'ticket:create', 'ticket:read', 'ticket:update', 'ticket:moderate', 'ticket:assign',
  'ticket:type:create', 'ticket:type:update',
  'ticket:status:create', 'ticket:status:update',
  'ticket:priority:create', 'ticket:priority:update',
  'sla:create', 'sla:update',
  'project:create', 'project:update',
  'user:read'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Map TENANT_ADMIN permissions (all except system)
INSERT INTO role_permissions (role_id, permission_id, is_active)
SELECT r.id, p.id, true FROM roles r, permissions p
WHERE r.code = 'tenant_admin' AND p.category != 'SYSTEM'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Map SYSTEM_ADMIN permissions (all)
INSERT INTO role_permissions (role_id, permission_id, is_active)
SELECT r.id, p.id, true FROM roles r, permissions p
WHERE r.code = 'system_admin'
ON CONFLICT (role_id, permission_id) DO NOTHING;
