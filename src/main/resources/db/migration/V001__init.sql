-- =============================================================================
-- V001__init.sql — Full schema + seed data
-- =============================================================================

-- ── tenants ───────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tenants (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    domain     VARCHAR(255) NOT NULL UNIQUE,
    status     VARCHAR(50)  NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_tenants_domain ON tenants(domain);

-- ── customers ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS customers (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    trade_name  VARCHAR(255),
    document    VARCHAR(50)  NOT NULL,
    email       VARCHAR(255),
    phone       VARCHAR(20),
    address     TEXT,
    city        VARCHAR(100),
    state       VARCHAR(2),
    logo_url    VARCHAR(500),
    notes       TEXT,
    status      VARCHAR(50)  NOT NULL DEFAULT 'active',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ,
    UNIQUE(tenant_id, document)
);
CREATE INDEX IF NOT EXISTS idx_customers_tenant_id ON customers(tenant_id);
CREATE INDEX IF NOT EXISTS idx_customers_status    ON customers(status);

-- ── users ─────────────────────────────────────────────────────────────────────
-- type: admin | agent = internal staff of the tenant
--       end_user      = contact of a customer who submits tickets
CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID         REFERENCES tenants(id) ON DELETE CASCADE,
    customer_id     UUID         REFERENCES customers(id) ON DELETE SET NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255),
    name            VARCHAR(255),
    avatar_url      VARCHAR(500),
    type            VARCHAR(50)  NOT NULL DEFAULT 'agent',
    role            VARCHAR(50)  NOT NULL DEFAULT 'agent',
    email_confirmed BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_users_tenant_id   ON users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_users_customer_id ON users(customer_id);
CREATE INDEX IF NOT EXISTS idx_users_email       ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_type        ON users(type);

-- ── refresh_tokens ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      TEXT        NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token   ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- ── email_confirmation_tokens ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS email_confirmation_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_email_confirmation_tokens_token   ON email_confirmation_tokens(token);
CREATE INDEX IF NOT EXISTS idx_email_confirmation_tokens_user_id ON email_confirmation_tokens(user_id);

-- ── password_reset_tokens ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_token ON password_reset_tokens(token);

-- ── roles ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(100) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    hierarchy_level INT          NOT NULL DEFAULT 0,
    description     TEXT,
    is_system       BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ── permissions ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS permissions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    category    VARCHAR(50)  NOT NULL,
    description TEXT,
    is_system   BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_permissions_code     ON permissions(code);
CREATE INDEX IF NOT EXISTS idx_permissions_category ON permissions(category);

-- ── role_permissions ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS role_permissions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id       UUID    NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID    NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(role_id, permission_id)
);
CREATE INDEX IF NOT EXISTS idx_role_permissions_role_id       ON role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission_id ON role_permissions(permission_id);

-- ── user_roles ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id     UUID    NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    assigned_by UUID REFERENCES users(id),
    revoked_at  TIMESTAMPTZ,
    revoked_by  UUID REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(user_id, role_id)
);
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id   ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id   ON user_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_is_active ON user_roles(is_active);

-- ── resource_permissions ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS resource_permissions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_id   UUID        NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    user_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action        VARCHAR(50) NOT NULL,
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    granted_by    VARCHAR(100),
    expires_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(resource_id, user_id, action, resource_type)
);
CREATE INDEX IF NOT EXISTS idx_resource_permissions_resource ON resource_permissions(resource_id, resource_type);
CREATE INDEX IF NOT EXISTS idx_resource_permissions_user     ON resource_permissions(user_id);

-- ── agent_groups ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS agent_groups (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_agent_groups_tenant_id ON agent_groups(tenant_id);
CREATE INDEX IF NOT EXISTS idx_agent_groups_is_active ON agent_groups(is_active);

-- ── agent_group_members ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS agent_group_members (
    group_id    UUID NOT NULL REFERENCES agent_groups(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (group_id, user_id)
);

-- ── slas ──────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS slas (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name                     VARCHAR(255) NOT NULL,
    response_time_hours      INT          NOT NULL,
    resolution_time_hours    INT          NOT NULL,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_slas_tenant_id ON slas(tenant_id);

-- ── ticket_statuses ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ticket_statuses (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    color      VARCHAR(7)   NOT NULL,
    is_default BOOLEAN      NOT NULL DEFAULT FALSE,
    is_closed  BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order INT          NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_ticket_statuses_tenant_id  ON ticket_statuses(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ticket_statuses_sort_order ON ticket_statuses(sort_order);

-- ── ticket_types ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ticket_types (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    icon        VARCHAR(50),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_ticket_types_tenant_id ON ticket_types(tenant_id);

-- ── ticket_priorities ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ticket_priorities (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    color           VARCHAR(7)   NOT NULL,
    sla_multiplier  NUMERIC(4,2) NOT NULL DEFAULT 1.0,
    sort_order      INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_ticket_priorities_tenant_id  ON ticket_priorities(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ticket_priorities_sort_order ON ticket_priorities(sort_order);

-- ── categories ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS categories (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    parent_id   UUID REFERENCES categories(id),
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ
);

-- ── systems ───────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS systems (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    customer_id UUID         NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    version     VARCHAR(100),
    url         VARCHAR(500),
    status      VARCHAR(50)  NOT NULL DEFAULT 'active',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_systems_customer_id ON systems(customer_id);

-- ── projects ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS projects (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                   VARCHAR(255) NOT NULL,
    description            TEXT,
    system_id              UUID         NOT NULL REFERENCES systems(id) ON DELETE CASCADE,
    github_repository      VARCHAR(255),
    github_owner           VARCHAR(255),
    pull_request_status_id UUID REFERENCES ticket_statuses(id),
    status                 VARCHAR(50)  NOT NULL DEFAULT 'active',
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_projects_system_id ON projects(system_id);

-- ── project_members ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS project_members (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(50),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ,
    UNIQUE(project_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_project_members_project_id ON project_members(project_id);
CREATE INDEX IF NOT EXISTS idx_project_members_user_id    ON project_members(user_id);

-- ── customer_accesses ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS customer_accesses (
    id           UUID PRIMARY KEY,
    customer_id  UUID        NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    access_level VARCHAR(50),
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_customer_accesses_customer_id ON customer_accesses(customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_accesses_user_id     ON customer_accesses(user_id);

-- ── tickets ───────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tickets (
    id                     UUID PRIMARY KEY,
    code                   BIGINT      NOT NULL UNIQUE,
    title                  VARCHAR(255) NOT NULL,
    description            TEXT,
    private_notes          TEXT,
    customer_id            UUID        NOT NULL REFERENCES customers(id),
    status_id              UUID REFERENCES ticket_statuses(id),
    priority               VARCHAR(50) NOT NULL DEFAULT 'medium',
    type                   VARCHAR(50) NOT NULL DEFAULT 'support',
    created_by             UUID        NOT NULL REFERENCES users(id),
    assigned_to            UUID REFERENCES users(id),
    sla_id                 UUID        NOT NULL REFERENCES slas(id),
    group_id               UUID REFERENCES agent_groups(id),
    priority_id            UUID REFERENCES ticket_priorities(id),
    type_id                UUID REFERENCES ticket_types(id),
    project_id             UUID REFERENCES projects(id),
    system_id              UUID REFERENCES systems(id),
    github_pull_request_url VARCHAR(500),
    visibility             VARCHAR(50) NOT NULL DEFAULT 'PROJECT',
    created_at             TIMESTAMPTZ NOT NULL,
    closed_at              TIMESTAMPTZ,
    updated_at             TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_tickets_customer_id ON tickets(customer_id);
CREATE INDEX IF NOT EXISTS idx_tickets_status_id   ON tickets(status_id);
CREATE INDEX IF NOT EXISTS idx_tickets_created_at  ON tickets(created_at);
CREATE INDEX IF NOT EXISTS idx_tickets_visibility  ON tickets(visibility);
CREATE INDEX IF NOT EXISTS idx_tickets_code        ON tickets(code);
CREATE INDEX IF NOT EXISTS idx_tickets_project_id  ON tickets(project_id);
CREATE INDEX IF NOT EXISTS idx_tickets_system_id   ON tickets(system_id);

-- ── ticket_categories (join table) ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ticket_categories (
    ticket_id   UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (ticket_id, category_id)
);

-- ── knowledge_base ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS knowledge_base (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    title        VARCHAR(255) NOT NULL,
    content      TEXT,
    category_id  UUID REFERENCES categories(id),
    author_id    UUID REFERENCES users(id),
    is_published BOOLEAN     NOT NULL DEFAULT FALSE,
    views        INT         NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_knowledge_base_tenant_id    ON knowledge_base(tenant_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_base_is_published ON knowledge_base(is_published);

-- ── ticket_knowledge_base (join table) ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ticket_knowledge_base (
    ticket_id  UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    article_id UUID NOT NULL REFERENCES knowledge_base(id) ON DELETE CASCADE,
    PRIMARY KEY (ticket_id, article_id)
);

-- ── ticket_interactions ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ticket_interactions (
    id               UUID PRIMARY KEY,
    ticket_id        UUID        NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    user_id          UUID REFERENCES users(id),
    message          TEXT,
    interaction_type VARCHAR(50),
    visibility       VARCHAR(50) NOT NULL DEFAULT 'PROJECT',
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_ticket_interactions_ticket_id  ON ticket_interactions(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_interactions_created_at ON ticket_interactions(created_at);

-- ── attachments ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS attachments (
    id          UUID PRIMARY KEY,
    file_name   VARCHAR(255) NOT NULL,
    file_url    VARCHAR(500) NOT NULL,
    file_size   BIGINT,
    mime_type   VARCHAR(100),
    uploaded_by UUID REFERENCES users(id),
    created_at  TIMESTAMPTZ  NOT NULL
);

-- ── ticket_attachments ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ticket_attachments (
    id            UUID PRIMARY KEY,
    ticket_id     UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    attachment_id UUID NOT NULL REFERENCES attachments(id) ON DELETE CASCADE,
    created_at    TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_ticket_attachments_ticket_id ON ticket_attachments(ticket_id);



-- ── ticket_homologations ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ticket_homologations (
    id         UUID PRIMARY KEY,
    ticket_id  UUID        NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES users(id),
    status     VARCHAR(50) NOT NULL DEFAULT 'pending',
    notes      TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    UNIQUE(ticket_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_ticket_homologations_ticket_id ON ticket_homologations(ticket_id);

-- ── github_configuration ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS github_configuration (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id     UUID        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    webhook_secret VARCHAR(255),
    enabled        BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_github_configuration_project_id ON github_configuration(project_id);

-- ── audit_events ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_events (
    id          UUID PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id   UUID         NOT NULL,
    action      VARCHAR(50)  NOT NULL,
    user_id     UUID REFERENCES users(id),
    changes     TEXT,
    ticket_id   UUID REFERENCES tickets(id),
    created_at  TIMESTAMPTZ  NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_audit_events_entity        ON audit_events(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_ticket        ON audit_events(ticket_id, created_at DESC);

-- ── time_entries ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS time_entries (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id   UUID        NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users(id),
    hours       NUMERIC(6,2) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_time_entries_ticket_id ON time_entries(ticket_id);
CREATE INDEX IF NOT EXISTS idx_time_entries_user_id   ON time_entries(user_id);

-- =============================================================================
-- Seed: default roles
-- =============================================================================
INSERT INTO roles (code, name, hierarchy_level, description, is_system, is_active, created_at, updated_at) VALUES
    ('client',       'Client',       0, 'End user',              TRUE, TRUE, now(), now()),
    ('developer',    'Developer',    1, 'Support agent',         TRUE, TRUE, now(), now()),
    ('moderator',    'Moderator',    2, 'Moderator',             TRUE, TRUE, now(), now()),
    ('tenant_admin', 'Tenant Admin', 3, 'Tenant administrator',  TRUE, TRUE, now(), now()),
    ('system_admin', 'System Admin', 4, 'System administrator',  TRUE, TRUE, now(), now())
ON CONFLICT (code) DO NOTHING;

-- =============================================================================
-- Seed: default permissions
-- =============================================================================
INSERT INTO permissions (code, name, category, is_system, created_at, updated_at) VALUES
    ('ticket:create',           'Create Ticket',          'TICKET',      TRUE, now(), now()),
    ('ticket:read',             'Read Ticket',            'TICKET',      TRUE, now(), now()),
    ('ticket:update',           'Update Ticket',          'TICKET',      TRUE, now(), now()),
    ('ticket:delete',           'Delete Ticket',          'TICKET',      TRUE, now(), now()),
    ('ticket:assign',           'Assign Ticket',          'TICKET',      TRUE, now(), now()),
    ('ticket:moderate',         'Moderate Ticket',        'TICKET',      TRUE, now(), now()),
    ('ticket:type:create',      'Create Ticket Type',     'SETUP',       TRUE, now(), now()),
    ('ticket:type:update',      'Update Ticket Type',     'SETUP',       TRUE, now(), now()),
    ('ticket:status:create',    'Create Ticket Status',   'SETUP',       TRUE, now(), now()),
    ('ticket:status:update',    'Update Ticket Status',   'SETUP',       TRUE, now(), now()),
    ('ticket:priority:create',  'Create Priority',        'SETUP',       TRUE, now(), now()),
    ('ticket:priority:update',  'Update Priority',        'SETUP',       TRUE, now(), now()),
    ('sla:create',              'Create SLA',             'SETUP',       TRUE, now(), now()),
    ('sla:update',              'Update SLA',             'SETUP',       TRUE, now(), now()),
    ('project:create',          'Create Project',         'SETUP',       TRUE, now(), now()),
    ('project:update',          'Update Project',         'SETUP',       TRUE, now(), now()),
    ('project:delete',          'Delete Project',         'SETUP',       TRUE, now(), now()),
    ('integration:create',      'Create Integration',     'INTEGRATION', TRUE, now(), now()),
    ('integration:read',        'Read Integration',       'INTEGRATION', TRUE, now(), now()),
    ('integration:update',      'Update Integration',     'INTEGRATION', TRUE, now(), now()),
    ('integration:delete',      'Delete Integration',     'INTEGRATION', TRUE, now(), now()),
    ('integration:execute',     'Execute Integration',    'INTEGRATION', TRUE, now(), now()),
    ('user:create',             'Create User',            'USER',        TRUE, now(), now()),
    ('user:read',               'Read User',              'USER',        TRUE, now(), now()),
    ('user:update',             'Update User',            'USER',        TRUE, now(), now()),
    ('user:delete',             'Delete User',            'USER',        TRUE, now(), now()),
    ('role:create',             'Create Role',            'ROLE',        TRUE, now(), now()),
    ('role:read',               'Read Role',              'ROLE',        TRUE, now(), now()),
    ('role:update',             'Update Role',            'ROLE',        TRUE, now(), now()),
    ('role:delete',             'Delete Role',            'ROLE',        TRUE, now(), now()),
    ('system:read',             'Read System Config',     'SYSTEM',      TRUE, now(), now()),
    ('system:update',           'Update System Config',   'SYSTEM',      TRUE, now(), now())
ON CONFLICT (code) DO NOTHING;

-- =============================================================================
-- Seed: role → permission mappings
-- =============================================================================
-- client
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'client' AND p.code IN ('ticket:create','ticket:read')
ON CONFLICT DO NOTHING;

-- developer
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'developer' AND p.code IN (
    'ticket:create','ticket:read','ticket:update','ticket:delete',
    'ticket:assign','ticket:moderate',
    'integration:read','integration:execute','integration:delete'
) ON CONFLICT DO NOTHING;

-- moderator
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'moderator' AND p.code IN (
    'ticket:create','ticket:read','ticket:update','ticket:delete',
    'ticket:assign','ticket:moderate',
    'ticket:type:create','ticket:type:update',
    'ticket:status:create','ticket:status:update',
    'ticket:priority:create','ticket:priority:update',
    'sla:create','sla:update',
    'project:create','project:update',
    'user:read','user:update'
) ON CONFLICT DO NOTHING;

-- tenant_admin (all except SYSTEM)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'tenant_admin' AND p.category != 'SYSTEM'
ON CONFLICT DO NOTHING;

-- system_admin (all)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'system_admin'
ON CONFLICT DO NOTHING;

-- =============================================================================
-- Seed: bootstrap admin from env vars (via Java migration V002)
-- =============================================================================
