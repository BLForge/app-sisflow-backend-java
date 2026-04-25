-- Create base tables for SisFlow

-- Create customers table
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    trade_name VARCHAR(255),
    document VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255),
    phone VARCHAR(20),
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(2),
    logo_url VARCHAR(500),
    notes TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_customers_document ON customers(document);
CREATE INDEX IF NOT EXISTS idx_customers_status ON customers(status);

-- Create user_profiles table
CREATE TABLE IF NOT EXISTS user_profiles (
    id UUID PRIMARY KEY,
    customer_id UUID,
    name VARCHAR(255),
    avatar_url VARCHAR(500),
    role VARCHAR(50) NOT NULL DEFAULT 'client',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_user_profiles_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE INDEX IF NOT EXISTS idx_user_profiles_customer_id ON user_profiles(customer_id);
CREATE INDEX IF NOT EXISTS idx_user_profiles_role ON user_profiles(role);

-- Create ticket_statuses table
CREATE TABLE IF NOT EXISTS ticket_statuses (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    color VARCHAR(7) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT false,
    is_closed BOOLEAN NOT NULL DEFAULT false,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_ticket_statuses_name ON ticket_statuses(name);
CREATE INDEX IF NOT EXISTS idx_ticket_statuses_sort_order ON ticket_statuses(sort_order);

-- Create ticket_types table
CREATE TABLE IF NOT EXISTS ticket_types (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon VARCHAR(50),
    color VARCHAR(7),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_ticket_types_name ON ticket_types(name);

-- Create ticket_priorities table
CREATE TABLE IF NOT EXISTS ticket_priorities (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    level INT NOT NULL,
    color VARCHAR(7),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_ticket_priorities_level ON ticket_priorities(level);

-- Create agent_groups table (moved before tickets since tickets references it)
CREATE TABLE IF NOT EXISTS agent_groups (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_agent_groups_is_active ON agent_groups(is_active);

-- Create slas table (moved before tickets since tickets references it)
CREATE TABLE IF NOT EXISTS slas (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    response_time_minutes INT,
    resolution_time_minutes INT,
    priority_id UUID,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_slas_is_active ON slas(is_active);

-- Create tickets table
CREATE TABLE IF NOT EXISTS tickets (
    id UUID PRIMARY KEY,
    code BIGINT NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    private_notes TEXT,
    customer_id UUID NOT NULL,
    status_id UUID,
    priority VARCHAR(50) NOT NULL DEFAULT 'medium',
    type VARCHAR(50) NOT NULL DEFAULT 'support',
    created_by UUID NOT NULL,
    assigned_to UUID,
    sla_id UUID NOT NULL,
    group_id UUID,
    priority_id UUID,
    type_id UUID,
    project_id UUID,
    system_id UUID,
    github_pull_request_url VARCHAR(500),
    visibility VARCHAR(50) NOT NULL DEFAULT 'PROJECT',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_tickets_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_tickets_status FOREIGN KEY (status_id) REFERENCES ticket_statuses(id),
    CONSTRAINT fk_tickets_created_by FOREIGN KEY (created_by) REFERENCES user_profiles(id),
    CONSTRAINT fk_tickets_assigned_to FOREIGN KEY (assigned_to) REFERENCES user_profiles(id),
    CONSTRAINT fk_tickets_sla FOREIGN KEY (sla_id) REFERENCES slas(id),
    CONSTRAINT fk_tickets_group FOREIGN KEY (group_id) REFERENCES agent_groups(id),
    CONSTRAINT fk_tickets_priority FOREIGN KEY (priority_id) REFERENCES ticket_priorities(id),
    CONSTRAINT fk_tickets_type FOREIGN KEY (type_id) REFERENCES ticket_types(id),
    CONSTRAINT fk_tickets_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_tickets_system FOREIGN KEY (system_id) REFERENCES systems(id)
);

CREATE INDEX IF NOT EXISTS idx_tickets_customer_id ON tickets(customer_id);
CREATE INDEX IF NOT EXISTS idx_tickets_status_id ON tickets(status_id);
CREATE INDEX IF NOT EXISTS idx_tickets_queue_id ON tickets(group_id);
CREATE INDEX IF NOT EXISTS idx_tickets_created_at ON tickets(created_at);
CREATE INDEX IF NOT EXISTS idx_tickets_visibility ON tickets(visibility);
CREATE INDEX IF NOT EXISTS idx_tickets_code ON tickets(code);

-- Create ticket_interactions table
CREATE TABLE IF NOT EXISTS ticket_interactions (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL,
    user_id UUID,
    message TEXT,
    interaction_type VARCHAR(50),
    visibility VARCHAR(50) DEFAULT 'PROJECT',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_ticket_interactions_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ticket_interactions_ticket_id ON ticket_interactions(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_interactions_created_at ON ticket_interactions(created_at);
CREATE INDEX IF NOT EXISTS idx_ticket_interactions_visibility ON ticket_interactions(visibility);

-- Create attachments table
CREATE TABLE IF NOT EXISTS attachments (
    id UUID PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(100),
    uploaded_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_attachments_uploaded_by ON attachments(uploaded_by);

-- Create ticket_attachments table
CREATE TABLE IF NOT EXISTS ticket_attachments (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL,
    attachment_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ticket_attachments_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_ticket_attachments_attachment FOREIGN KEY (attachment_id) REFERENCES attachments(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ticket_attachments_ticket_id ON ticket_attachments(ticket_id);

-- Create categories table
CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    parent_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id)
);

CREATE INDEX IF NOT EXISTS idx_categories_parent_id ON categories(parent_id);

-- Create knowledge_base table
CREATE TABLE IF NOT EXISTS knowledge_base (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    category_id UUID,
    author_id UUID,
    is_published BOOLEAN DEFAULT false,
    views INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_knowledge_base_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE INDEX IF NOT EXISTS idx_knowledge_base_category_id ON knowledge_base(category_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_base_is_published ON knowledge_base(is_published);

-- Create agent_group_members table
CREATE TABLE IF NOT EXISTS agent_group_members (
    agent_group_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (agent_group_id, user_id),
    CONSTRAINT fk_agent_group_members_group FOREIGN KEY (agent_group_id) REFERENCES agent_groups(id) ON DELETE CASCADE
);

-- Create audit_events table
CREATE TABLE IF NOT EXISTS audit_events (
    id UUID PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    user_id UUID,
    changes TEXT,
    ticket_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_events_entity ON audit_events(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_ticket_created ON audit_events(ticket_id, created_at DESC);

-- Create status_transitions table
CREATE TABLE IF NOT EXISTS status_transitions (
    id UUID PRIMARY KEY,
    from_status_id UUID,
    to_status_id UUID NOT NULL,
    is_allowed BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_status_transitions_from FOREIGN KEY (from_status_id) REFERENCES ticket_statuses(id),
    CONSTRAINT fk_status_transitions_to FOREIGN KEY (to_status_id) REFERENCES ticket_statuses(id)
);

-- Create ticket_homologations table
CREATE TABLE IF NOT EXISTS ticket_homologations (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_ticket_homologations_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT uk_ticket_homologations UNIQUE (ticket_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_ticket_homologations_ticket_id ON ticket_homologations(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_homologations_user_id ON ticket_homologations(user_id);

-- Create github_configuration table
CREATE TABLE IF NOT EXISTS github_configuration (
    id UUID PRIMARY KEY,
    owner VARCHAR(255) NOT NULL,
    repository VARCHAR(255) NOT NULL,
    access_token VARCHAR(500),
    webhook_url VARCHAR(500),
    webhook_secret VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_github_configuration_owner_repo ON github_configuration(owner, repository);

-- Create projects table
CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    customer_id UUID NOT NULL,
    github_repository VARCHAR(255),
    github_owner VARCHAR(255),
    pull_request_status_id UUID,
    status VARCHAR(50) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_projects_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_projects_pull_request_status FOREIGN KEY (pull_request_status_id) REFERENCES ticket_statuses(id)
);

CREATE INDEX IF NOT EXISTS idx_projects_customer_id ON projects(customer_id);
CREATE INDEX IF NOT EXISTS idx_projects_github ON projects(github_owner, github_repository);

-- Create systems table
CREATE TABLE IF NOT EXISTS systems (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    project_id UUID NOT NULL,
    version VARCHAR(100),
    url VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_systems_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_systems_project_id ON systems(project_id);

-- Create project_members table
CREATE TABLE IF NOT EXISTS project_members (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_project_members_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT uk_project_members UNIQUE (project_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_project_members_project_id ON project_members(project_id);
CREATE INDEX IF NOT EXISTS idx_project_members_user_id ON project_members(user_id);

-- Create customer_accesses table
CREATE TABLE IF NOT EXISTS customer_accesses (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    user_id UUID NOT NULL,
    access_level VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_customer_accesses_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_customer_accesses_customer_id ON customer_accesses(customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_accesses_user_id ON customer_accesses(user_id);

-- Create resource_permissions table
CREATE TABLE IF NOT EXISTS resource_permissions (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    user_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_resource_permissions UNIQUE (resource_id, user_id, action, resource_type)
);

CREATE INDEX IF NOT EXISTS idx_resource_permissions_resource ON resource_permissions(resource_id, resource_type);
CREATE INDEX IF NOT EXISTS idx_resource_permissions_user ON resource_permissions(user_id);
