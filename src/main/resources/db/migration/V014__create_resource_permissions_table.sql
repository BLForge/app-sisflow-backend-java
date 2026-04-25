CREATE TABLE IF NOT EXISTS resource_permissions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  resource_id UUID NOT NULL,
  resource_type VARCHAR(50) NOT NULL,
  user_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
  action VARCHAR(50) NOT NULL,
  is_active BOOLEAN DEFAULT true,
  granted_by VARCHAR(100),
  expires_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT now(),
  UNIQUE(resource_id, user_id, action, resource_type)
);

CREATE INDEX idx_resource_permissions_resource_id ON resource_permissions(resource_id);
CREATE INDEX idx_resource_permissions_user_id ON resource_permissions(user_id);
CREATE INDEX idx_resource_permissions_resource_type ON resource_permissions(resource_type);
CREATE INDEX idx_resource_permissions_expires_at ON resource_permissions(expires_at);
