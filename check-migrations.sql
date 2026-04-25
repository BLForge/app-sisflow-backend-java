-- Migration verification script
-- Run this in your PostgreSQL database to check migration status

-- 1. Check Flyway schema history
SELECT version, description, type, installed_on, success 
FROM flyway_schema_history 
ORDER BY installed_rank DESC 
LIMIT 10;

-- 2. Check if visibility columns exist
SELECT 
    table_name,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name IN ('tickets', 'ticket_interactions') 
    AND column_name = 'visibility';

-- 3. Check if RLS functions exist
SELECT 
    routine_name,
    routine_type,
    routine_schema
FROM information_schema.routines 
WHERE routine_name IN (
    'get_current_user_id',
    'get_current_user_hierarchy_level',
    'has_permission',
    'is_admin_or_above',
    'is_moderator_or_above'
);

-- 4. Check if RLS is enabled on tables
SELECT 
    schemaname,
    tablename,
    rowsecurity
FROM pg_tables 
WHERE tablename IN ('customers', 'user_profiles', 'tickets', 'ticket_interactions')
    AND schemaname = 'public';

-- 5. Check if auth schema exists
SELECT schema_name 
FROM information_schema.schemata 
WHERE schema_name = 'auth';

-- 6. Check base tables exist
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
    AND table_name IN ('customers', 'user_profiles', 'tickets', 'ticket_interactions', 'roles', 'permissions', 'user_roles')
ORDER BY table_name;

-- 7. Check for any failed migrations
SELECT version, description, checksum, installed_on, execution_time, success
FROM flyway_schema_history 
WHERE success = false;