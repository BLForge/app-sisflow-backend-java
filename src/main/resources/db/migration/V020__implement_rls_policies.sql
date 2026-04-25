-- Implement Row-Level Security (RLS) for role-based access control
-- This migration creates RLS policies based on the role hierarchy

-- ============================================================================
-- HELPER FUNCTIONS FOR RLS
-- ============================================================================

-- Function to get current user ID from JWT claims
CREATE OR REPLACE FUNCTION auth.get_current_user_id()
RETURNS UUID AS $$
BEGIN
  RETURN NULLIF(current_setting('app.current_user_id', true), '')::UUID;
END;
$$ LANGUAGE plpgsql STABLE;

-- Function to get current user role from JWT claims
CREATE OR REPLACE FUNCTION auth.get_current_user_role()
RETURNS VARCHAR AS $$
BEGIN
  RETURN NULLIF(current_setting('app.current_user_role', true), '');
END;
$$ LANGUAGE plpgsql STABLE;

-- Function to get current user hierarchy level
CREATE OR REPLACE FUNCTION auth.get_current_user_hierarchy_level()
RETURNS INT AS $$
DECLARE
  v_level INT;
BEGIN
  SELECT COALESCE(MAX(r.hierarchy_level), -1)
  INTO v_level
  FROM user_roles ur
  JOIN roles r ON ur.role_id = r.id
  WHERE ur.user_id = auth.get_current_user_id()
    AND ur.is_active = true;
  
  RETURN v_level;
END;
$$ LANGUAGE plpgsql STABLE;

-- Function to check if user has permission
CREATE OR REPLACE FUNCTION auth.has_permission(p_permission_code VARCHAR)
RETURNS BOOLEAN AS $$
BEGIN
  RETURN EXISTS (
    SELECT 1
    FROM user_roles ur
    JOIN roles r ON ur.role_id = r.id
    JOIN role_permissions rp ON r.id = rp.role_id
    JOIN permissions p ON rp.permission_id = p.id
    WHERE ur.user_id = auth.get_current_user_id()
      AND ur.is_active = true
      AND r.is_active = true
      AND p.is_active = true
      AND p.code = p_permission_code
  );
END;
$$ LANGUAGE plpgsql STABLE;

-- Function to check if user is admin or above
CREATE OR REPLACE FUNCTION auth.is_admin_or_above()
RETURNS BOOLEAN AS $$
BEGIN
  RETURN auth.get_current_user_hierarchy_level() >= 4; -- SYSTEM_ADMIN = 4
END;
$$ LANGUAGE plpgsql STABLE;

-- Function to check if user is moderator or above
CREATE OR REPLACE FUNCTION auth.is_moderator_or_above()
RETURNS BOOLEAN AS $$
BEGIN
  RETURN auth.get_current_user_hierarchy_level() >= 2; -- MODERATOR = 2
END;
$$ LANGUAGE plpgsql STABLE;

-- Function to check if user is developer or above
CREATE OR REPLACE FUNCTION auth.is_developer_or_above()
RETURNS BOOLEAN AS $$
BEGIN
  RETURN auth.get_current_user_hierarchy_level() >= 1; -- DEVELOPER = 1
END;
$$ LANGUAGE plpgsql STABLE;

-- ============================================================================
-- ENABLE RLS ON TABLES
-- ============================================================================

ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE tickets ENABLE ROW LEVEL SECURITY;
ALTER TABLE ticket_interactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE ticket_attachments ENABLE ROW LEVEL SECURITY;
ALTER TABLE attachments ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE permissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE role_permissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE projects ENABLE ROW LEVEL SECURITY;
ALTER TABLE systems ENABLE ROW LEVEL SECURITY;
ALTER TABLE project_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_accesses ENABLE ROW LEVEL SECURITY;
ALTER TABLE agent_groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE agent_group_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE knowledge_base ENABLE ROW LEVEL SECURITY;
ALTER TABLE slas ENABLE ROW LEVEL SECURITY;
ALTER TABLE ticket_statuses ENABLE ROW LEVEL SECURITY;
ALTER TABLE ticket_types ENABLE ROW LEVEL SECURITY;
ALTER TABLE ticket_priorities ENABLE ROW LEVEL SECURITY;
ALTER TABLE ticket_homologations ENABLE ROW LEVEL SECURITY;
ALTER TABLE status_transitions ENABLE ROW LEVEL SECURITY;
ALTER TABLE resource_permissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_events ENABLE ROW LEVEL SECURITY;

-- ============================================================================
-- CUSTOMERS TABLE POLICIES
-- ============================================================================

-- Admins can see all customers
CREATE POLICY customers_admin_select ON customers
FOR SELECT
USING (auth.is_admin_or_above());

-- Moderators and above can see customers they have access to
CREATE POLICY customers_moderator_select ON customers
FOR SELECT
USING (
  auth.is_moderator_or_above()
  OR EXISTS (
    SELECT 1 FROM customer_accesses ca
    WHERE ca.customer_id = customers.id
      AND ca.user_id = auth.get_current_user_id()
  )
);

-- Only admins can create customers
CREATE POLICY customers_admin_insert ON customers
FOR INSERT
WITH CHECK (auth.is_admin_or_above());

-- Only admins can update customers
CREATE POLICY customers_admin_update ON customers
FOR UPDATE
USING (auth.is_admin_or_above());

-- Only admins can delete customers
CREATE POLICY customers_admin_delete ON customers
FOR DELETE
USING (auth.is_admin_or_above());

-- ============================================================================
-- USER_PROFILES TABLE POLICIES
-- ============================================================================

-- Users can see their own profile
CREATE POLICY user_profiles_self_select ON user_profiles
FOR SELECT
USING (id = auth.get_current_user_id());

-- Admins can see all user profiles
CREATE POLICY user_profiles_admin_select ON user_profiles
FOR SELECT
USING (auth.is_admin_or_above());

-- Moderators can see user profiles in their customer
CREATE POLICY user_profiles_moderator_select ON user_profiles
FOR SELECT
USING (
  auth.is_moderator_or_above()
  AND customer_id IN (
    SELECT ca.customer_id FROM customer_accesses ca
    WHERE ca.user_id = auth.get_current_user_id()
  )
);

-- Users can update their own profile
CREATE POLICY user_profiles_self_update ON user_profiles
FOR UPDATE
USING (id = auth.get_current_user_id());

-- Only admins can create user profiles
CREATE POLICY user_profiles_admin_insert ON user_profiles
FOR INSERT
WITH CHECK (auth.is_admin_or_above());

-- Only admins can delete user profiles
CREATE POLICY user_profiles_admin_delete ON user_profiles
FOR DELETE
USING (auth.is_admin_or_above());

-- ============================================================================
-- TICKETS TABLE POLICIES
-- ============================================================================

-- Users can see tickets they created
CREATE POLICY tickets_creator_select ON tickets
FOR SELECT
USING (created_by = auth.get_current_user_id());

-- Users can see tickets assigned to them
CREATE POLICY tickets_assigned_select ON tickets
FOR SELECT
USING (assigned_to = auth.get_current_user_id());

-- Admins can see all tickets
CREATE POLICY tickets_admin_select ON tickets
FOR SELECT
USING (auth.is_admin_or_above());

-- Moderators can see tickets in their customer
CREATE POLICY tickets_moderator_select ON tickets
FOR SELECT
USING (
  auth.is_moderator_or_above()
  AND customer_id IN (
    SELECT ca.customer_id FROM customer_accesses ca
    WHERE ca.user_id = auth.get_current_user_id()
  )
);

-- Users can create tickets
CREATE POLICY tickets_user_insert ON tickets
FOR INSERT
WITH CHECK (
  created_by = auth.get_current_user_id()
  AND auth.has_permission('ticket:create')
);

-- Users can update tickets they created
CREATE POLICY tickets_creator_update ON tickets
FOR UPDATE
USING (created_by = auth.get_current_user_id())
WITH CHECK (auth.has_permission('ticket:update'));

-- Users can update tickets assigned to them
CREATE POLICY tickets_assigned_update ON tickets
FOR UPDATE
USING (assigned_to = auth.get_current_user_id())
WITH CHECK (auth.has_permission('ticket:update'));

-- Moderators can update any ticket in their customer
CREATE POLICY tickets_moderator_update ON tickets
FOR UPDATE
USING (
  auth.is_moderator_or_above()
  AND customer_id IN (
    SELECT ca.customer_id FROM customer_accesses ca
    WHERE ca.user_id = auth.get_current_user_id()
  )
)
WITH CHECK (auth.has_permission('ticket:update'));

-- Only moderators can delete tickets
CREATE POLICY tickets_moderator_delete ON tickets
FOR DELETE
USING (
  auth.is_moderator_or_above()
  AND auth.has_permission('ticket:delete')
);

-- ============================================================================
-- TICKET_INTERACTIONS TABLE POLICIES
-- ============================================================================

-- Users can see interactions on tickets they can see
CREATE POLICY ticket_interactions_select ON ticket_interactions
FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM tickets t
    WHERE t.id = ticket_interactions.ticket_id
      AND (
        t.created_by = auth.get_current_user_id()
        OR t.assigned_to = auth.get_current_user_id()
        OR auth.is_moderator_or_above()
      )
  )
);

-- Users can create interactions on tickets they can access
CREATE POLICY ticket_interactions_insert ON ticket_interactions
FOR INSERT
WITH CHECK (
  EXISTS (
    SELECT 1 FROM tickets t
    WHERE t.id = ticket_interactions.ticket_id
      AND (
        t.created_by = auth.get_current_user_id()
        OR t.assigned_to = auth.get_current_user_id()
        OR auth.is_moderator_or_above()
      )
  )
  AND auth.has_permission('ticket:update')
);

-- Users can update their own interactions
CREATE POLICY ticket_interactions_update ON ticket_interactions
FOR UPDATE
USING (user_id = auth.get_current_user_id())
WITH CHECK (auth.has_permission('ticket:update'));

-- Only moderators can delete interactions
CREATE POLICY ticket_interactions_delete ON ticket_interactions
FOR DELETE
USING (auth.is_moderator_or_above() AND auth.has_permission('ticket:delete'));

-- ============================================================================
-- ROLES TABLE POLICIES
-- ============================================================================

-- All authenticated users can see active roles
CREATE POLICY roles_select ON roles
FOR SELECT
USING (is_active = true);

-- Only admins can create roles
CREATE POLICY roles_admin_insert ON roles
FOR INSERT
WITH CHECK (auth.is_admin_or_above());

-- Only admins can update roles
CREATE POLICY roles_admin_update ON roles
FOR UPDATE
USING (auth.is_admin_or_above());

-- Only admins can delete roles
CREATE POLICY roles_admin_delete ON roles
FOR DELETE
USING (auth.is_admin_or_above());

-- ============================================================================
-- PERMISSIONS TABLE POLICIES
-- ============================================================================

-- All authenticated users can see active permissions
CREATE POLICY permissions_select ON permissions
FOR SELECT
USING (is_active = true);

-- Only admins can create permissions
CREATE POLICY permissions_admin_insert ON permissions
FOR INSERT
WITH CHECK (auth.is_admin_or_above());

-- Only admins can update permissions
CREATE POLICY permissions_admin_update ON permissions
FOR UPDATE
USING (auth.is_admin_or_above());

-- Only admins can delete permissions
CREATE POLICY permissions_admin_delete ON permissions
FOR DELETE
USING (auth.is_admin_or_above());

-- ============================================================================
-- USER_ROLES TABLE POLICIES
-- ============================================================================

-- Users can see their own roles
CREATE POLICY user_roles_self_select ON user_roles
FOR SELECT
USING (user_id = auth.get_current_user_id());

-- Admins can see all user roles
CREATE POLICY user_roles_admin_select ON user_roles
FOR SELECT
USING (auth.is_admin_or_above());

-- Only admins can create user roles
CREATE POLICY user_roles_admin_insert ON user_roles
FOR INSERT
WITH CHECK (auth.is_admin_or_above());

-- Only admins can update user roles
CREATE POLICY user_roles_admin_update ON user_roles
FOR UPDATE
USING (auth.is_admin_or_above());

-- Only admins can delete user roles
CREATE POLICY user_roles_admin_delete ON user_roles
FOR DELETE
USING (auth.is_admin_or_above());

-- ============================================================================
-- PROJECTS TABLE POLICIES
-- ============================================================================

-- Users can see projects they are members of
CREATE POLICY projects_member_select ON projects
FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM project_members pm
    WHERE pm.project_id = projects.id
      AND pm.user_id = auth.get_current_user_id()
  )
  OR auth.is_admin_or_above()
);

-- Only admins can create projects
CREATE POLICY projects_admin_insert ON projects
FOR INSERT
WITH CHECK (auth.is_admin_or_above());

-- Project members can update projects
CREATE POLICY projects_member_update ON projects
FOR UPDATE
USING (
  EXISTS (
    SELECT 1 FROM project_members pm
    WHERE pm.project_id = projects.id
      AND pm.user_id = auth.get_current_user_id()
  )
  OR auth.is_admin_or_above()
);

-- Only admins can delete projects
CREATE POLICY projects_admin_delete ON projects
FOR DELETE
USING (auth.is_admin_or_above());

-- ============================================================================
-- AGENT_GROUPS TABLE POLICIES
-- ============================================================================

-- Users can see agent groups they are members of
CREATE POLICY agent_groups_member_select ON agent_groups
FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM agent_group_members agm
    WHERE agm.agent_group_id = agent_groups.id
      AND agm.user_id = auth.get_current_user_id()
  )
  OR auth.is_moderator_or_above()
);

-- Only moderators can create agent groups
CREATE POLICY agent_groups_moderator_insert ON agent_groups
FOR INSERT
WITH CHECK (auth.is_moderator_or_above());

-- Only moderators can update agent groups
CREATE POLICY agent_groups_moderator_update ON agent_groups
FOR UPDATE
USING (auth.is_moderator_or_above());

-- Only moderators can delete agent groups
CREATE POLICY agent_groups_moderator_delete ON agent_groups
FOR DELETE
USING (auth.is_moderator_or_above());

-- ============================================================================
-- CATEGORIES TABLE POLICIES
-- ============================================================================

-- All authenticated users can see categories
CREATE POLICY categories_select ON categories
FOR SELECT
USING (true);

-- Only moderators can create categories
CREATE POLICY categories_moderator_insert ON categories
FOR INSERT
WITH CHECK (auth.is_moderator_or_above());

-- Only moderators can update categories
CREATE POLICY categories_moderator_update ON categories
FOR UPDATE
USING (auth.is_moderator_or_above());

-- Only moderators can delete categories
CREATE POLICY categories_moderator_delete ON categories
FOR DELETE
USING (auth.is_moderator_or_above());

-- ============================================================================
-- KNOWLEDGE_BASE TABLE POLICIES
-- ============================================================================

-- All authenticated users can see published articles
CREATE POLICY knowledge_base_select ON knowledge_base
FOR SELECT
USING (is_published = true OR author_id = auth.get_current_user_id() OR auth.is_moderator_or_above());

-- Developers and above can create articles
CREATE POLICY knowledge_base_insert ON knowledge_base
FOR INSERT
WITH CHECK (auth.is_developer_or_above());

-- Authors and moderators can update articles
CREATE POLICY knowledge_base_update ON knowledge_base
FOR UPDATE
USING (author_id = auth.get_current_user_id() OR auth.is_moderator_or_above());

-- Only moderators can delete articles
CREATE POLICY knowledge_base_delete ON knowledge_base
FOR DELETE
USING (auth.is_moderator_or_above());

-- ============================================================================
-- AUDIT_EVENTS TABLE POLICIES
-- ============================================================================

-- Users can see audit events for their own actions
CREATE POLICY audit_events_self_select ON audit_events
FOR SELECT
USING (user_id = auth.get_current_user_id());

-- Admins can see all audit events
CREATE POLICY audit_events_admin_select ON audit_events
FOR SELECT
USING (auth.is_admin_or_above());

-- Only the system can insert audit events
CREATE POLICY audit_events_system_insert ON audit_events
FOR INSERT
WITH CHECK (true);

-- ============================================================================
-- TICKET_STATUSES, TYPES, PRIORITIES TABLE POLICIES
-- ============================================================================

-- All authenticated users can see ticket configurations
CREATE POLICY ticket_statuses_select ON ticket_statuses
FOR SELECT
USING (true);

CREATE POLICY ticket_types_select ON ticket_types
FOR SELECT
USING (true);

CREATE POLICY ticket_priorities_select ON ticket_priorities
FOR SELECT
USING (true);

-- Only moderators can modify ticket configurations
CREATE POLICY ticket_statuses_moderator_insert ON ticket_statuses
FOR INSERT
WITH CHECK (auth.is_moderator_or_above());

CREATE POLICY ticket_statuses_moderator_update ON ticket_statuses
FOR UPDATE
USING (auth.is_moderator_or_above());

CREATE POLICY ticket_statuses_moderator_delete ON ticket_statuses
FOR DELETE
USING (auth.is_moderator_or_above());

CREATE POLICY ticket_types_moderator_insert ON ticket_types
FOR INSERT
WITH CHECK (auth.is_moderator_or_above());

CREATE POLICY ticket_types_moderator_update ON ticket_types
FOR UPDATE
USING (auth.is_moderator_or_above());

CREATE POLICY ticket_types_moderator_delete ON ticket_types
FOR DELETE
USING (auth.is_moderator_or_above());

CREATE POLICY ticket_priorities_moderator_insert ON ticket_priorities
FOR INSERT
WITH CHECK (auth.is_moderator_or_above());

CREATE POLICY ticket_priorities_moderator_update ON ticket_priorities
FOR UPDATE
USING (auth.is_moderator_or_above());

CREATE POLICY ticket_priorities_moderator_delete ON ticket_priorities
FOR DELETE
USING (auth.is_moderator_or_above());

-- ============================================================================
-- SLAS TABLE POLICIES
-- ============================================================================

-- All authenticated users can see SLAs
CREATE POLICY slas_select ON slas
FOR SELECT
USING (true);

-- Only moderators can create SLAs
CREATE POLICY slas_moderator_insert ON slas
FOR INSERT
WITH CHECK (auth.is_moderator_or_above());

-- Only moderators can update SLAs
CREATE POLICY slas_moderator_update ON slas
FOR UPDATE
USING (auth.is_moderator_or_above());

-- Only moderators can delete SLAs
CREATE POLICY slas_moderator_delete ON slas
FOR DELETE
USING (auth.is_moderator_or_above());

-- ============================================================================
-- ATTACHMENTS TABLE POLICIES
-- ============================================================================

-- Users can see attachments they uploaded
CREATE POLICY attachments_self_select ON attachments
FOR SELECT
USING (uploaded_by = auth.get_current_user_id());

-- Users can see attachments on tickets they can access
CREATE POLICY attachments_ticket_select ON attachments
FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM ticket_attachments ta
    JOIN tickets t ON ta.ticket_id = t.id
    WHERE ta.attachment_id = attachments.id
      AND (
        t.created_by = auth.get_current_user_id()
        OR t.assigned_to = auth.get_current_user_id()
        OR auth.is_moderator_or_above()
      )
  )
);

-- Users can upload attachments
CREATE POLICY attachments_insert ON attachments
FOR INSERT
WITH CHECK (uploaded_by = auth.get_current_user_id());

-- Users can delete their own attachments
CREATE POLICY attachments_self_delete ON attachments
FOR DELETE
USING (uploaded_by = auth.get_current_user_id());

-- ============================================================================
-- TICKET_ATTACHMENTS TABLE POLICIES
-- ============================================================================

-- Users can see ticket attachments for tickets they can access
CREATE POLICY ticket_attachments_select ON ticket_attachments
FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM tickets t
    WHERE t.id = ticket_attachments.ticket_id
      AND (
        t.created_by = auth.get_current_user_id()
        OR t.assigned_to = auth.get_current_user_id()
        OR auth.is_moderator_or_above()
      )
  )
);

-- Users can attach files to tickets they can access
CREATE POLICY ticket_attachments_insert ON ticket_attachments
FOR INSERT
WITH CHECK (
  EXISTS (
    SELECT 1 FROM tickets t
    WHERE t.id = ticket_attachments.ticket_id
      AND (
        t.created_by = auth.get_current_user_id()
        OR t.assigned_to = auth.get_current_user_id()
        OR auth.is_moderator_or_above()
      )
  )
);

-- Users can remove attachments from tickets they can access
CREATE POLICY ticket_attachments_delete ON ticket_attachments
FOR DELETE
USING (
  EXISTS (
    SELECT 1 FROM tickets t
    WHERE t.id = ticket_attachments.ticket_id
      AND (
        t.created_by = auth.get_current_user_id()
        OR t.assigned_to = auth.get_current_user_id()
        OR auth.is_moderator_or_above()
      )
  )
);

-- ============================================================================
-- REMAINING TABLES - PERMISSIVE POLICIES FOR SYSTEM OPERATIONS
-- ============================================================================

-- Role permissions - admins only
CREATE POLICY role_permissions_select ON role_permissions
FOR SELECT
USING (auth.is_admin_or_above());

CREATE POLICY role_permissions_insert ON role_permissions
FOR INSERT
WITH CHECK (auth.is_admin_or_above());

CREATE POLICY role_permissions_delete ON role_permissions
FOR DELETE
USING (auth.is_admin_or_above());

-- Project members - project members and admins
CREATE POLICY project_members_select ON project_members
FOR SELECT
USING (
  user_id = auth.get_current_user_id()
  OR auth.is_admin_or_above()
  OR EXISTS (
    SELECT 1 FROM project_members pm
    WHERE pm.project_id = project_members.project_id
      AND pm.user_id = auth.get_current_user_id()
  )
);

CREATE POLICY project_members_insert ON project_members
FOR INSERT
WITH CHECK (auth.is_admin_or_above());

CREATE POLICY project_members_delete ON project_members
FOR DELETE
USING (auth.is_admin_or_above());

-- Customer accesses - admins only
CREATE POLICY customer_accesses_select ON customer_accesses
FOR SELECT
USING (auth.is_admin_or_above());

CREATE POLICY customer_accesses_insert ON customer_accesses
FOR INSERT
WITH CHECK (auth.is_admin_or_above());

CREATE POLICY customer_accesses_delete ON customer_accesses
FOR DELETE
USING (auth.is_admin_or_above());

-- Agent group members - group members and admins
CREATE POLICY agent_group_members_select ON agent_group_members
FOR SELECT
USING (
  user_id = auth.get_current_user_id()
  OR auth.is_admin_or_above()
);

CREATE POLICY agent_group_members_insert ON agent_group_members
FOR INSERT
WITH CHECK (auth.is_moderator_or_above());

CREATE POLICY agent_group_members_delete ON agent_group_members
FOR DELETE
USING (auth.is_moderator_or_above());

-- Systems - project members can see
CREATE POLICY systems_select ON systems
FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM projects p
    WHERE p.id = systems.project_id
      AND EXISTS (
        SELECT 1 FROM project_members pm
        WHERE pm.project_id = p.id
          AND pm.user_id = auth.get_current_user_id()
      )
  )
  OR auth.is_admin_or_above()
);

CREATE POLICY systems_insert ON systems
FOR INSERT
WITH CHECK (auth.is_admin_or_above());

CREATE POLICY systems_update ON systems
FOR UPDATE
USING (auth.is_admin_or_above());

CREATE POLICY systems_delete ON systems
FOR DELETE
USING (auth.is_admin_or_above());

-- Ticket homologations - users involved and moderators
CREATE POLICY ticket_homologations_select ON ticket_homologations
FOR SELECT
USING (
  user_id = auth.get_current_user_id()
  OR auth.is_moderator_or_above()
  OR EXISTS (
    SELECT 1 FROM tickets t
    WHERE t.id = ticket_homologations.ticket_id
      AND (t.created_by = auth.get_current_user_id() OR t.assigned_to = auth.get_current_user_id())
  )
);

CREATE POLICY ticket_homologations_insert ON ticket_homologations
FOR INSERT
WITH CHECK (auth.is_moderator_or_above());

CREATE POLICY ticket_homologations_update ON ticket_homologations
FOR UPDATE
USING (user_id = auth.get_current_user_id() OR auth.is_moderator_or_above());

-- Status transitions - all can see, moderators can modify
CREATE POLICY status_transitions_select ON status_transitions
FOR SELECT
USING (true);

CREATE POLICY status_transitions_insert ON status_transitions
FOR INSERT
WITH CHECK (auth.is_moderator_or_above());

CREATE POLICY status_transitions_update ON status_transitions
FOR UPDATE
USING (auth.is_moderator_or_above());

CREATE POLICY status_transitions_delete ON status_transitions
FOR DELETE
USING (auth.is_moderator_or_above());

-- Resource permissions - admins only
CREATE POLICY resource_permissions_select ON resource_permissions
FOR SELECT
USING (user_id = auth.get_current_user_id() OR auth.is_admin_or_above());

CREATE POLICY resource_permissions_insert ON resource_permissions
FOR INSERT
WITH CHECK (auth.is_admin_or_above());

CREATE POLICY resource_permissions_delete ON resource_permissions
FOR DELETE
USING (auth.is_admin_or_above());
