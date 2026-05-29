-- Script para limpar e recomeçar com a nova estrutura
-- ATENCAO: isso remove os dados de projects, systems e github_configuration
-- Uso exclusivo em desenvolvimento

TRUNCATE TABLE github_configuration CASCADE;
TRUNCATE TABLE projects CASCADE;
TRUNCATE TABLE systems CASCADE;

ALTER TABLE systems DROP CONSTRAINT IF EXISTS fk_systems_project;
ALTER TABLE projects DROP CONSTRAINT IF EXISTS fk_projects_customer;

ALTER TABLE systems DROP COLUMN IF EXISTS project_id;
ALTER TABLE projects DROP COLUMN IF EXISTS customer_id;

ALTER TABLE systems ADD COLUMN IF NOT EXISTS customer_id UUID;
ALTER TABLE projects ADD COLUMN IF NOT EXISTS system_id UUID;

ALTER TABLE systems ALTER COLUMN customer_id SET NOT NULL;
ALTER TABLE projects ALTER COLUMN system_id SET NOT NULL;

ALTER TABLE systems ADD CONSTRAINT fk_systems_customer
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;

ALTER TABLE projects ADD CONSTRAINT fk_projects_system
    FOREIGN KEY (system_id) REFERENCES systems(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_systems_customer_id ON systems(customer_id);
CREATE INDEX IF NOT EXISTS idx_projects_system_id ON projects(system_id);
