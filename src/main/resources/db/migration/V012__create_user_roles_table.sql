CREATE TABLE IF NOT EXISTS user_roles (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
  role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
  is_active BOOLEAN DEFAULT true,
  assigned_at TIMESTAMP DEFAULT now(),
  assigned_by UUID REFERENCES user_profiles(id),
  revoked_at TIMESTAMP,
  revoked_by UUID REFERENCES user_profiles(id),
  created_at TIMESTAMP DEFAULT now(),
  UNIQUE(user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX idx_user_roles_is_active ON user_roles(is_active);
