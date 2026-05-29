-- ============================================================================
-- Script para adicionar customer_id em user_profiles
-- ============================================================================
-- Execute este script APENAS SE o Hibernate nao criar automaticamente
-- ou se voce quiser aplicar a mudanca antes de iniciar a aplicacao
-- ============================================================================

ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS customer_id UUID;

ALTER TABLE user_profiles ADD CONSTRAINT IF NOT EXISTS fk_user_profiles_customer
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_user_profiles_customer ON user_profiles(customer_id);
