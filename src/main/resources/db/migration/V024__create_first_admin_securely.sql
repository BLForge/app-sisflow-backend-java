-- V024: Create first admin user securely
-- This migration creates an admin user for the first registered user only
-- Run this migration, then immediately remove it after use

DO $
DECLARE
    first_user_id UUID;
    admin_role_id UUID;
    existing_admin_count INT;
BEGIN
    -- Check if any admin users already exist
    SELECT COUNT(*) INTO existing_admin_count 
    FROM user_roles ur 
    JOIN roles r ON ur.role_id = r.id 
    WHERE r.code = 'system_admin' AND ur.is_active = true;
    
    -- Only proceed if no admins exist
    IF existing_admin_count = 0 THEN
        -- Get the system admin role ID
        SELECT id INTO admin_role_id FROM roles WHERE code = 'system_admin';
        
        IF admin_role_id IS NULL THEN
            RAISE EXCEPTION 'System admin role not found';
        END IF;
        
        -- Get the first user from user_profiles (assuming this is you)
        SELECT id INTO first_user_id FROM user_profiles ORDER BY created_at ASC LIMIT 1;
        
        IF first_user_id IS NOT NULL THEN
            -- Assign admin role to first user
            INSERT INTO user_roles (id, user_id, role_id, is_active, assigned_at, created_at)
            VALUES (gen_random_uuid(), first_user_id, admin_role_id, true, now(), now())
            ON CONFLICT (user_id, role_id) DO NOTHING;
            
            RAISE NOTICE 'Assigned admin role to first user: %', first_user_id;
        ELSE
            RAISE NOTICE 'No users found in user_profiles table';
        END IF;
    ELSE
        RAISE NOTICE 'Admin users already exist, skipping admin creation';
    END IF;
END $;