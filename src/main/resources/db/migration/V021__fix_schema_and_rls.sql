-- Fix schema issues and implement RLS properly
-- This migration addresses the missing auth schema and visibility columns

-- ============================================================================
-- CREATE AUTH SCHEMA
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS auth;

-- ============================================================================
-- ENSURE VISIBILITY COLUMNS EXIST
-- ============================================================================

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

-- ============================================================================
-- HELPER FUNCTIONS FOR RLS (Fixed to use public schema initially)
-- ============================================================================

-- Function to get current user ID from session variables
CREATE OR REPLACE FUNCTION get_current_user_id()
RETURNS UUID AS $$
BEGIN
  RETURN NULLIF(current_setting('app.current_user_id', true), '')::UUID;
EXCEPTION
  WHEN OTHERS THEN
    RETURN NULL;
END;
$$ LANGUAGE plpgsql STABLE;

-- Function to get current user hierarchy level
CREATE OR REPLACE FUNCTION get_current_user_hierarchy_level()
RETURNS INT AS $$
DECLARE
  v_level INT;
BEGIN
  SELECT COALESCE(MAX(r.hierarchy_level), -1)
  INTO v_level
  FROM user_roles ur
  JOIN roles r ON ur.role_id = r.id
  WHERE ur.user_id = get_current_user_id()
    AND ur.is_active = true;
  
  RETURN v_level;
EXCEPTION
  WHEN OTHERS THEN
    RETURN -1;
END;
$$ LANGUAGE plpgsql STABLE;

-- Function to check if user has permission
CREATE OR REPLACE FUNCTION has_permission(p_permission_code VARCHAR)
RETURNS BOOLEAN AS $$
BEGIN
  RETURN EXISTS (
    SELECT 1
    FROM user_roles ur
    JOIN roles r ON ur.role_id = r.id
    JOIN role_permissions rp ON r.id = rp.role_id
    JOIN permissions p ON rp.permission_id = p.id
    WHERE ur.user_id = get_current_user_id()
      AND ur.is_active = true
      AND r.is_active = true
      AND p.is_active = true
      AND p.code = p_permission_code
  );
EXCEPTION
  WHEN OTHERS THEN
    RETURN false;
END;
$$ LANGUAGE plpgsql STABLE;

-- Function to check if user is admin or above
CREATE OR REPLACE FUNCTION is_admin_or_above()
RETURNS BOOLEAN AS $$
BEGIN
  RETURN get_current_user_hierarchy_level() >= 4; -- SYSTEM_ADMIN = 4
END;
$$ LANGUAGE plpgsql STABLE;

-- Function to check if user is moderator or above
CREATE OR REPLACE FUNCTION is_moderator_or_above()
RETURNS BOOLEAN AS $$
BEGIN
  RETURN get_current_user_hierarchy_level() >= 2; -- MODERATOR = 2
END;
$$ LANGUAGE plpgsql STABLE;

-- Function to check if user is developer or above
CREATE OR REPLACE FUNCTION is_developer_or_above()
RETURNS BOOLEAN AS $$
BEGIN
  RETURN get_current_user_hierarchy_level() >= 1; -- DEVELOPER = 1
END;
$$ LANGUAGE plpgsql STABLE;

-- ============================================================================
-- ENABLE RLS ON CORE TABLES (Safe approach)
-- ============================================================================

DO $$
BEGIN
    -- Enable RLS only on tables that exist
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'customers') THEN
        ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_profiles') THEN
        ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'tickets') THEN
        ALTER TABLE tickets ENABLE ROW LEVEL SECURITY;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ticket_interactions') THEN
        ALTER TABLE ticket_interactions ENABLE ROW LEVEL SECURITY;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'roles') THEN
        ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'permissions') THEN
        ALTER TABLE permissions ENABLE ROW LEVEL SECURITY;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_roles') THEN
        ALTER TABLE user_roles ENABLE ROW LEVEL SECURITY;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'projects') THEN
        ALTER TABLE projects ENABLE ROW LEVEL SECURITY;
    END IF;
END $$;

-- ============================================================================
-- BASIC RLS POLICIES (Essential ones only)
-- ============================================================================

-- Drop existing policies if they exist (to avoid conflicts)
DROP POLICY IF EXISTS customers_admin_select ON customers;
DROP POLICY IF EXISTS tickets_creator_select ON tickets;
DROP POLICY IF EXISTS tickets_assigned_select ON tickets;
DROP POLICY IF EXISTS tickets_admin_select ON tickets;
DROP POLICY IF EXISTS user_profiles_self_select ON user_profiles;
DROP POLICY IF EXISTS user_profiles_admin_select ON user_profiles;

-- CUSTOMERS TABLE POLICIES
CREATE POLICY customers_admin_select ON customers
FOR SELECT
USING (is_admin_or_above() OR get_current_user_id() IS NOT NULL);

-- USER_PROFILES TABLE POLICIES
CREATE POLICY user_profiles_self_select ON user_profiles
FOR SELECT
USING (id = get_current_user_id() OR is_admin_or_above());

-- TICKETS TABLE POLICIES
CREATE POLICY tickets_creator_select ON tickets
FOR SELECT
USING (
    created_by = get_current_user_id() 
    OR assigned_to = get_current_user_id()
    OR is_moderator_or_above()
);

CREATE POLICY tickets_user_insert ON tickets
FOR INSERT
WITH CHECK (created_by = get_current_user_id());

CREATE POLICY tickets_update ON tickets
FOR UPDATE
USING (
    created_by = get_current_user_id() 
    OR assigned_to = get_current_user_id()
    OR is_moderator_or_above()
);

-- TICKET_INTERACTIONS TABLE POLICIES
CREATE POLICY ticket_interactions_select ON ticket_interactions
FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM tickets t
    WHERE t.id = ticket_interactions.ticket_id
      AND (
        t.created_by = get_current_user_id()
        OR t.assigned_to = get_current_user_id()
        OR is_moderator_or_above()
      )
  )
);

CREATE POLICY ticket_interactions_insert ON ticket_interactions
FOR INSERT
WITH CHECK (
  EXISTS (
    SELECT 1 FROM tickets t
    WHERE t.id = ticket_interactions.ticket_id
      AND (
        t.created_by = get_current_user_id()
        OR t.assigned_to = get_current_user_id()
        OR is_moderator_or_above()
      )
  )
);

-- ROLES TABLE POLICIES
CREATE POLICY roles_select ON roles
FOR SELECT
USING (is_active = true);

-- PERMISSIONS TABLE POLICIES  
CREATE POLICY permissions_select ON permissions
FOR SELECT
USING (is_active = true);

-- USER_ROLES TABLE POLICIES
CREATE POLICY user_roles_self_select ON user_roles
FOR SELECT
USING (user_id = get_current_user_id() OR is_admin_or_above());

-- PROJECTS TABLE POLICIES
CREATE POLICY projects_select ON projects
FOR SELECT
USING (is_admin_or_above() OR get_current_user_id() IS NOT NULL);

-- ============================================================================
-- GRANT PERMISSIONS
-- ============================================================================

-- Grant usage on auth schema
GRANT USAGE ON SCHEMA auth TO PUBLIC;

-- Grant execute on functions
GRANT EXECUTE ON FUNCTION get_current_user_id() TO PUBLIC;
GRANT EXECUTE ON FUNCTION get_current_user_hierarchy_level() TO PUBLIC;
GRANT EXECUTE ON FUNCTION has_permission(VARCHAR) TO PUBLIC;
GRANT EXECUTE ON FUNCTION is_admin_or_above() TO PUBLIC;
GRANT EXECUTE ON FUNCTION is_moderator_or_above() TO PUBLIC;
GRANT EXECUTE ON FUNCTION is_developer_or_above() TO PUBLIC;
