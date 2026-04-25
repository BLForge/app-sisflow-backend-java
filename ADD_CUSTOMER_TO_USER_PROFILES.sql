-- ============================================================================
-- Script para adicionar customer_id em user_profiles
-- ============================================================================
-- Execute este script APENAS SE o Hibernate não criar automaticamente
-- ou se você quiser aplicar a mudança antes de iniciar a aplicação
-- ============================================================================

-- Adicionar coluna customer_id em user_profiles
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS customer_id UUID;

-- Adicionar foreign key para customers
ALTER TABLE user_profiles ADD CONSTRAINT IF NOT EXISTS fk_user_profiles_customer 
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL;

-- Criar índice para performance
CREATE INDEX IF NOT EXISTS idx_user_profiles_customer ON user_profiles(customer_id);

-- ============================================================================
-- Verificar se funcionou:
-- SELECT column_name, data_type FROM information_schema.columns 
-- WHERE table_name = 'user_profiles' AND column_name = 'customer_id';
-- ============================================================================
