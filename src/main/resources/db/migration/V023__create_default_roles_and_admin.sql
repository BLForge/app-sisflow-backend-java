-- Create default roles and admin user
-- This ensures the system has basic roles and at least one admin user

-- Create auth schema if it doesn't exist (for RLS functions later)
CREATE SCHEMA IF NOT EXISTS auth;

-- Insert default roles if they don't exist
INSERT INTO roles (id, name, code, hierarchy_level, description, is_system, is_active, created_at, updated_at)
VALUES 
    (gen_random_uuid(), 'Client', 'client', 0, 'Basic client access', true, true, now(), now()),
    (gen_random_uuid(), 'Developer', 'developer', 1, 'Developer access', true, true, now(), now()),
    (gen_random_uuid(), 'Moderator', 'moderator', 2, 'Moderator access', true, true, now(), now()),
    (gen_random_uuid(), 'Tenant Admin', 'tenant_admin', 3, 'Tenant administrator', true, true, now(), now()),
    (gen_random_uuid(), 'System Admin', 'system_admin', 4, 'System administrator', true, true, now(), now())
ON CONFLICT (code) DO NOTHING;

-- Insert default permissions if they don't exist
INSERT INTO permissions (id, code, name, description, category, is_system, is_active, created_at, updated_at)
VALUES 
    -- Ticket permissions
    (gen_random_uuid(), 'ticket:create', 'Create Tickets', 'Can create new tickets', 'TICKET', true, true, now(), now()),
    (gen_random_uuid(), 'ticket:update', 'Update Tickets', 'Can update tickets', 'TICKET', true, true, now(), now()),
    (gen_random_uuid(), 'ticket:delete', 'Delete Tickets', 'Can delete tickets', 'TICKET', true, true, now(), now()),
    (gen_random_uuid(), 'ticket:view', 'View Tickets', 'Can view tickets', 'TICKET', true, true, now(), now()),
    
    -- Customer permissions
    (gen_random_uuid(), 'customer:create', 'Create Customers', 'Can create customers', 'CUSTOMER', true, true, now(), now()),
    (gen_random_uuid(), 'customer:update', 'Update Customers', 'Can update customers', 'CUSTOMER', true, true, now(), now()),
    (gen_random_uuid(), 'customer:delete', 'Delete Customers', 'Can delete customers', 'CUSTOMER', true, true, now(), now()),
    (gen_random_uuid(), 'customer:view', 'View Customers', 'Can view customers', 'CUSTOMER', true, true, now(), now()),
    
    -- System permissions
    (gen_random_uuid(), 'system:create', 'Create Systems', 'Can create systems', 'SYSTEM', true, true, now(), now()),
    (gen_random_uuid(), 'system:update', 'Update Systems', 'Can update systems', 'SYSTEM', true, true, now(), now()),
    (gen_random_uuid(), 'system:delete', 'Delete Systems', 'Can delete systems', 'SYSTEM', true, true, now(), now()),
    (gen_random_uuid(), 'system:view', 'View Systems', 'Can view systems', 'SYSTEM', true, true, now(), now()),
    
    -- Project permissions
    (gen_random_uuid(), 'project:create', 'Create Projects', 'Can create projects', 'PROJECT', true, true, now(), now()),
    (gen_random_uuid(), 'project:update', 'Update Projects', 'Can update projects', 'PROJECT', true, true, now(), now()),
    (gen_random_uuid(), 'project:delete', 'Delete Projects', 'Can delete projects', 'PROJECT', true, true, now(), now()),
    (gen_random_uuid(), 'project:view', 'View Projects', 'Can view projects', 'PROJECT', true, true, now(), now()),
    
    -- SLA permissions
    (gen_random_uuid(), 'sla:create', 'Create SLAs', 'Can create SLAs', 'SLA', true, true, now(), now()),
    (gen_random_uuid(), 'sla:update', 'Update SLAs', 'Can update SLAs', 'SLA', true, true, now(), now()),
    (gen_random_uuid(), 'sla:delete', 'Delete SLAs', 'Can delete SLAs', 'SLA', true, true, now(), now()),
    (gen_random_uuid(), 'sla:view', 'View SLAs', 'Can view SLAs', 'SLA', true, true, now(), now()),
    
    -- Role management permissions
    (gen_random_uuid(), 'role:create', 'Create Roles', 'Can create roles', 'ROLE', true, true, now(), now()),
    (gen_random_uuid(), 'role:update', 'Update Roles', 'Can update roles', 'ROLE', true, true, now(), now()),
    (gen_random_uuid(), 'role:delete', 'Delete Roles', 'Can delete roles', 'ROLE', true, true, now(), now()),
    (gen_random_uuid(), 'role:assign', 'Assign Roles', 'Can assign roles to users', 'ROLE', true, true, now(), now()),
    
    -- User management permissions
    (gen_random_uuid(), 'user:create', 'Create Users', 'Can create users', 'USER', true, true, now(), now()),
    (gen_random_uuid(), 'user:update', 'Update Users', 'Can update users', 'USER', true, true, now(), now()),
    (gen_random_uuid(), 'user:delete', 'Delete Users', 'Can delete users', 'USER', true, true, now(), now()),
    (gen_random_uuid(), 'user:view', 'View Users', 'Can view users', 'USER', true, true, now(), now())
ON CONFLICT (code) DO NOTHING;

-- Assign permissions to roles
DO $$
DECLARE
    client_role_id UUID;
    developer_role_id UUID;
    moderator_role_id UUID;
    tenant_admin_role_id UUID;
    system_admin_role_id UUID;
BEGIN
    -- Get role IDs
    SELECT id INTO client_role_id FROM roles WHERE code = 'client';
    SELECT id INTO developer_role_id FROM roles WHERE code = 'developer';
    SELECT id INTO moderator_role_id FROM roles WHERE code = 'moderator';
    SELECT id INTO tenant_admin_role_id FROM roles WHERE code = 'tenant_admin';
    SELECT id INTO system_admin_role_id FROM roles WHERE code = 'system_admin';
    
    -- Client permissions (basic ticket access)
    INSERT INTO role_permissions (id, role_id, permission_id, created_at)
    SELECT gen_random_uuid(), client_role_id, p.id, now()
    FROM permissions p 
    WHERE p.code IN ('ticket:create', 'ticket:update', 'ticket:view')
    ON CONFLICT (role_id, permission_id) DO NOTHING;
    
    -- Developer permissions (client + knowledge base)
    INSERT INTO role_permissions (id, role_id, permission_id, created_at)
    SELECT gen_random_uuid(), developer_role_id, p.id, now()
    FROM permissions p 
    WHERE p.code IN ('ticket:create', 'ticket:update', 'ticket:view')
    ON CONFLICT (role_id, permission_id) DO NOTHING;
    
    -- Moderator permissions (developer + SLA, groups, ticket config)
    INSERT INTO role_permissions (id, role_id, permission_id, created_at)
    SELECT gen_random_uuid(), moderator_role_id, p.id, now()
    FROM permissions p 
    WHERE p.code IN (
        'ticket:create', 'ticket:update', 'ticket:view', 'ticket:delete',
        'sla:create', 'sla:update', 'sla:view', 'sla:delete',
        'user:view', 'user:update'
    )
    ON CONFLICT (role_id, permission_id) DO NOTHING;
    
    -- Tenant Admin permissions (moderator + customer management)
    INSERT INTO role_permissions (id, role_id, permission_id, created_at)
    SELECT gen_random_uuid(), tenant_admin_role_id, p.id, now()
    FROM permissions p 
    WHERE p.code IN (
        'ticket:create', 'ticket:update', 'ticket:view', 'ticket:delete',
        'sla:create', 'sla:update', 'sla:view', 'sla:delete',
        'customer:create', 'customer:update', 'customer:view', 'customer:delete',
        'user:create', 'user:update', 'user:view', 'user:delete'
    )
    ON CONFLICT (role_id, permission_id) DO NOTHING;
    
    -- System Admin permissions (all permissions)
    INSERT INTO role_permissions (id, role_id, permission_id, created_at)
    SELECT gen_random_uuid(), system_admin_role_id, p.id, now()
    FROM permissions p
    ON CONFLICT (role_id, permission_id) DO NOTHING;
END $$;

-- Create a default admin user if no admin exists
-- This will use a placeholder UUID that you can replace with your actual user ID
DO $$
DECLARE
    admin_role_id UUID;
    existing_admin_count INT;
BEGIN
    -- Get system admin role ID
    SELECT id INTO admin_role_id FROM roles WHERE code = 'system_admin';
    
    -- Check if there are any existing admin users
    SELECT COUNT(*) INTO existing_admin_count 
    FROM user_roles ur 
    JOIN roles r ON ur.role_id = r.id 
    WHERE r.code = 'system_admin' AND ur.is_active = true;
    
    -- If no admin exists, we'll need to manually assign admin role to a user
    -- For now, just ensure the role structure exists
    IF existing_admin_count = 0 THEN
        -- Log that manual admin assignment is needed
        RAISE NOTICE 'No admin users found. You will need to manually assign the system_admin role to your user.';
        RAISE NOTICE 'Run this SQL with your user ID: INSERT INTO user_roles (id, user_id, role_id, is_active, created_at) VALUES (gen_random_uuid(), ''YOUR_USER_UUID'', ''%'', true, now());', admin_role_id;
    END IF;
END $$;