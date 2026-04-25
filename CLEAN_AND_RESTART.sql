-- Script para limpar e recomeçar com a nova estrutura
-- ATENÇÃO: Isso vai DELETAR TODOS OS DADOS de projects, systems e github_configuration!
-- Use apenas em desenvolvimento!

-- 1. Deletar todos os dados (ordem importa por causa das foreign keys)
TRUNCATE TABLE github_configuration CASCADE;
TRUNCATE TABLE projects CASCADE;
TRUNCATE TABLE systems CASCADE;

-- 2. Remover constraints antigas
ALTER TABLE systems DROP CONSTRAINT IF EXISTS fk_systems_project;
ALTER TABLE projects DROP CONSTRAINT IF EXISTS fk_projects_customer;

-- 3. Remover colunas antigas
ALTER TABLE systems DROP COLUMN IF EXISTS project_id;
ALTER TABLE projects DROP COLUMN IF EXISTS customer_id;

-- 4. Adicionar novas colunas
ALTER TABLE systems ADD COLUMN IF NOT EXISTS customer_id UUID;
ALTER TABLE projects ADD COLUMN IF NOT EXISTS system_id UUID;

-- 5. Tornar NOT NULL
ALTER TABLE systems ALTER COLUMN customer_id SET NOT NULL;
ALTER TABLE projects ALTER COLUMN system_id SET NOT NULL;

-- 6. Adicionar foreign keys
ALTER TABLE systems ADD CONSTRAINT fk_systems_customer 
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;

ALTER TABLE projects ADD CONSTRAINT fk_projects_system 
    FOREIGN KEY (system_id) REFERENCES systems(id) ON DELETE CASCADE;

-- 7. Criar índices
CREATE INDEX IF NOT EXISTS idx_systems_customer_id ON systems(customer_id);
CREATE INDEX IF NOT EXISTS idx_projects_system_id ON projects(system_id);

-- Pronto! Agora você pode criar novos sistemas e projetos com a hierarquia correta
