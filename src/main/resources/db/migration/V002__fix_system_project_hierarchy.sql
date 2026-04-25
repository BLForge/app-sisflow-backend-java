-- Migration para inverter hierarquia: Sistema pertence a Cliente, Projeto pertence a Sistema

-- 1. Remover constraint de project_id em systems (se existir)
ALTER TABLE systems DROP CONSTRAINT IF EXISTS fk_systems_project;

-- 2. Adicionar coluna customer_id em systems (se não existir)
ALTER TABLE systems ADD COLUMN IF NOT EXISTS customer_id UUID;

-- 3. Migrar dados: copiar customer_id dos projects para systems
-- Apenas se a coluna project_id ainda existe
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'systems' AND column_name = 'project_id') THEN
        UPDATE systems s
        SET customer_id = p.customer_id
        FROM projects p
        WHERE s.project_id = p.id
        AND s.customer_id IS NULL;
    END IF;
END $$;

-- 4. Se ainda houver systems sem customer_id, atribuir a um cliente padrão
-- Pega o primeiro cliente disponível
UPDATE systems 
SET customer_id = (SELECT id FROM customers LIMIT 1)
WHERE customer_id IS NULL;

-- 5. Tornar customer_id NOT NULL
ALTER TABLE systems ALTER COLUMN customer_id SET NOT NULL;

-- 6. Adicionar foreign key
ALTER TABLE systems ADD CONSTRAINT fk_systems_customer 
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;

-- 7. Remover coluna project_id de systems
ALTER TABLE systems DROP COLUMN IF EXISTS project_id;

-- 8. Adicionar coluna system_id em projects (se não existir)
ALTER TABLE projects ADD COLUMN IF NOT EXISTS system_id UUID;

-- 9. Migrar dados: mapear projects para systems
-- Criar um sistema para cada projeto existente se não existir
INSERT INTO systems (id, name, description, customer_id, status, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    'Sistema ' || p.name,
    p.description,
    p.customer_id,
    'active',
    NOW(),
    NOW()
FROM projects p
WHERE NOT EXISTS (SELECT 1 FROM systems s WHERE s.customer_id = p.customer_id)
ON CONFLICT DO NOTHING;

-- Atualizar projects com system_id
UPDATE projects p
SET system_id = (
    SELECT s.id 
    FROM systems s 
    WHERE s.customer_id = p.customer_id 
    LIMIT 1
)
WHERE system_id IS NULL;

-- 10. Remover constraint de customer_id em projects
ALTER TABLE projects DROP CONSTRAINT IF EXISTS fk_projects_customer;

-- 11. Adicionar foreign key de system_id em projects
ALTER TABLE projects ADD CONSTRAINT fk_projects_system 
    FOREIGN KEY (system_id) REFERENCES systems(id) ON DELETE CASCADE;

-- 12. Remover coluna customer_id de projects
ALTER TABLE projects DROP COLUMN IF EXISTS customer_id;

-- 13. Criar índices
CREATE INDEX IF NOT EXISTS idx_systems_customer_id ON systems(customer_id);
CREATE INDEX IF NOT EXISTS idx_projects_system_id ON projects(system_id);
