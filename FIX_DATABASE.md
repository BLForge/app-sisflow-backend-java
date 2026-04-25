# Como Corrigir o Banco de Dados

## Problema

O banco de dados ainda tem a estrutura antiga:
- `systems.project_id` (NOT NULL) ❌
- `projects.customer_id` (NOT NULL) ❌

Mas o código espera a nova estrutura:
- `systems.customer_id` (NOT NULL) ✅
- `projects.system_id` (NOT NULL) ✅

## Solução Rápida (Desenvolvimento)

Se você está em **desenvolvimento** e pode perder os dados de systems/projects:

### Opção 1: Via psql

```bash
# Conectar ao banco
psql -U postgres -d sisflow

# Copiar e colar o conteúdo de CLEAN_AND_RESTART.sql
\i CLEAN_AND_RESTART.sql

# Ou executar linha por linha
```

### Opção 2: Via DBeaver/PgAdmin

1. Conectar ao banco `sisflow`
2. Abrir o arquivo `CLEAN_AND_RESTART.sql`
3. Executar todo o script
4. Verificar se não há erros

### Opção 3: Via PowerShell

```powershell
# Ajuste as variáveis conforme seu ambiente
$env:PGPASSWORD="sua-senha"
psql -U postgres -d sisflow -f CLEAN_AND_RESTART.sql
```

## Solução Completa (Produção)

Se você está em **produção** e precisa manter os dados:

1. **Fazer backup:**
```bash
pg_dump -U postgres sisflow > backup_antes_migration.sql
```

2. **Executar migration:**
```bash
psql -U postgres -d sisflow -f src/main/resources/db/migration/V002__fix_system_project_hierarchy.sql
```

3. **Ajustar dados manualmente:**
   - Decidir como mapear projects existentes para systems
   - Pode precisar criar systems manualmente
   - Atualizar `projects.system_id` para apontar para os systems corretos

## Verificar se Funcionou

```sql
-- Verificar estrutura de systems
\d systems

-- Deve ter:
-- customer_id UUID NOT NULL
-- Não deve ter: project_id

-- Verificar estrutura de projects
\d projects

-- Deve ter:
-- system_id UUID NOT NULL
-- Não deve ter: customer_id
```

## Testar

Depois de executar a migration:

1. **Reiniciar o backend:**
```powershell
# Parar (Ctrl+C)
# Iniciar novamente
.\mvnw.cmd spring-boot:run
```

2. **Testar criar sistema:**
   - Ir em http://localhost:5173/app/systems
   - Clicar em "Novo Sistema"
   - Selecionar um cliente
   - Preencher nome, versão, etc
   - Salvar
   - Deve funcionar! ✅

3. **Testar criar projeto:**
   - Ir em http://localhost:5173/app/projects
   - Clicar em "Novo Projeto"
   - Selecionar um sistema
   - Preencher nome, GitHub, etc
   - Salvar
   - Deve funcionar! ✅

## Se Ainda Não Funcionar

### Verificar se a migration foi aplicada:

```sql
-- Ver colunas de systems
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'systems';

-- Deve mostrar customer_id e NÃO project_id

-- Ver colunas de projects
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'projects';

-- Deve mostrar system_id e NÃO customer_id
```

### Verificar constraints:

```sql
-- Ver foreign keys de systems
SELECT constraint_name, table_name, column_name 
FROM information_schema.key_column_usage 
WHERE table_name = 'systems';

-- Deve ter fk_systems_customer

-- Ver foreign keys de projects
SELECT constraint_name, table_name, column_name 
FROM information_schema.key_column_usage 
WHERE table_name = 'projects';

-- Deve ter fk_projects_system
```

## Alternativa: Recriar Tudo

Se nada funcionar e você está em desenvolvimento:

```sql
-- CUIDADO: Isso deleta TUDO!
DROP TABLE IF EXISTS github_configuration CASCADE;
DROP TABLE IF EXISTS projects CASCADE;
DROP TABLE IF EXISTS systems CASCADE;

-- Reiniciar o backend
-- O Hibernate vai recriar as tabelas com a estrutura correta
```

## Conectar ao Banco

### Local (Docker):
```bash
docker exec -it sisflow-postgres psql -U postgres -d sisflow
```

### Local (PostgreSQL instalado):
```bash
psql -U postgres -d sisflow
```

### Railway:
```bash
# Copiar DATABASE_URL do Railway
# Formato: postgresql://user:pass@host:port/db
psql "postgresql://user:pass@host:port/db"
```

## Resumo

1. ✅ Fazer backup (se produção)
2. ✅ Executar `CLEAN_AND_RESTART.sql` (desenvolvimento)
3. ✅ Reiniciar backend
4. ✅ Testar criar sistema
5. ✅ Testar criar projeto
6. ✅ Comemorar! 🎉
