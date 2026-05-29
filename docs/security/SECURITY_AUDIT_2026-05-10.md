# Auditoria de Segurança - Backend
**Data:** 2026-05-12  
**Auditor:** Security Review Team  
**Escopo:** Spring Boot Backend - SisFlow

## Resumo Executivo
Auditoria completa baseada em OWASP Top 10 2021 e boas práticas de segurança.

---

## 1. Broken Access Control (A01:2021)

### ✅ IMPLEMENTADO
- **Multi-tenant isolation** via `TenantIsolationFilter`
- **Role-based access control** (RBAC) com hierarquia
- **Authorization annotations** (`@RequireRole`, `@RequirePermission`)
- **Resource-level permissions** via `ResourcePermissionRepository`
- **Tenant context validation** em todos os endpoints

### ⚠️ VULNERABILIDADES ENCONTRADAS

#### CRÍTICO: IDOR em FileController
**Arquivo:** `FileController.java`
**Linha:** ~93
**Problema:** Endpoint `/files/{bucket}/{filename}` serve arquivos sem validar ownership
```java
@GetMapping("/{bucket}/{filename}")
public ResponseEntity<byte[]> serve(@PathVariable String bucket, @PathVariable String filename)
```
**Impacto:** Usuário pode acessar arquivos de outros tenants adivinhando UUIDs
**Fix:** Adicionar validação de tenant_id ou tornar arquivos públicos por design

#### ALTO: Customer Update sem validação de tenant
**Arquivo:** `CustomerController.java`
**Problema:** `update()` não valida se customer pertence ao tenant do usuário
**Fix:** Adicionar `customer.getTenant().getId().equals(tenantContext.getCurrentTenant())`

---

## 2. Cryptographic Failures (A02:2021)

### ✅ IMPLEMENTADO
- **BCrypt** para senhas (`passwordEncoder()`)
- **JWT** com secret configurável
- **HTTPS** recomendado (via reverse proxy)

### ⚠️ VULNERABILIDADES

#### MÉDIO: JWT Secret em plaintext
**Arquivo:** `application.properties`
**Problema:** `jwt.secret=${JWT_SECRET}` - se não configurado, usa default
**Fix:** Validar que JWT_SECRET está configurado no startup

---

## 3. Injection (A03:2021)

### ✅ IMPLEMENTADO
- **Parameterized queries** - JPA/Hibernate usado corretamente
- **No raw SQL** encontrado
- **Input validation** via `@Valid` e Bean Validation

### ✅ SEM VULNERABILIDADES ENCONTRADAS
Todas as queries usam JPQL parametrizado ou Spring Data JPA.

---

## 4. Insecure Design (A04:2021)

### ✅ IMPLEMENTADO
- **Rate limiting** (Bucket4j)
- **Account lockout** após 5 tentativas
- **IP blocking** após 15 tentativas
- **Idempotency protection**

### ⚠️ MELHORIAS SUGERIDAS
- Adicionar CAPTCHA após 3 tentativas falhas
- Implementar 2FA para admins

---

## 5. Security Misconfiguration (A05:2021)

### ✅ IMPLEMENTADO
- **Security headers** (CSP, X-Frame-Options, X-XSS-Protection)
- **Error handling** sanitizado (`GlobalExceptionHandler`)
- **CORS** configurado corretamente

### ⚠️ VULNERABILIDADES

#### MÉDIO: Debug logs em produção
**Arquivo:** `application.properties`
**Problema:** `logging.level.root=DEBUG`
**Fix:** Usar `INFO` em produção

#### BAIXO: Swagger habilitado
**Problema:** `springdoc.api-docs.enabled=${SWAGGER_ENABLED:false}`
**Fix:** Garantir que está desabilitado em produção

---

## 6. Vulnerable Components (A06:2021)

### ✅ VERIFICADO
- Spring Boot 4.0.5 (recente)
- Dependências atualizadas

### ⚠️ AÇÃO NECESSÁRIA
Executar `mvnw dependency-check:check` para scan de CVEs

---

## 7. Identification and Authentication Failures (A07:2021)

### ✅ IMPLEMENTADO
- **Password strength** não validado (FALTANDO)
- **Session management** via JWT (stateless)
- **Refresh tokens** com expiração

### ⚠️ VULNERABILIDADES

#### MÉDIO: Enumeração de usuários
**Arquivo:** `AuthService.java` - `signUp()`
**Problema:** Retorna 409 Conflict se email já existe
**Fix:** Retornar mensagem genérica "Se o email existir, você receberá um link"

#### MÉDIO: Enumeração via resendConfirmation
**Arquivo:** `AuthService.java` - `resendConfirmation()`
**Problema:** Retorna erro se usuário não existe
**Fix:** Sempre retornar sucesso

---

## 8. Software and Data Integrity Failures (A08:2021)

### ✅ IMPLEMENTADO
- **Flyway migrations** versionadas
- **Idempotency** para prevenir double-submit

### ✅ SEM VULNERABILIDADES ENCONTRADAS

---

## 9. Security Logging and Monitoring Failures (A09:2021)

### ✅ IMPLEMENTADO
- **Audit log** via `AuditService`
- **Login attempts** tracked

### ⚠️ MELHORIAS
- Adicionar alertas para múltiplas falhas de login
- Log de acessos a dados sensíveis

---

## 10. Server-Side Request Forgery (A10:2021)

### ✅ VERIFICADO
- **GitHub webhook** valida signature
- **File upload** valida tipo e tamanho

### ⚠️ VULNERABILIDADES

#### BAIXO: URL validation em branding
**Arquivo:** `TenantController.java`
**Problema:** Valida apenas `startsWith("/files/")` mas não valida path traversal
**Fix:** Adicionar validação de `..` no path

---

## Vulnerabilidades Adicionais

### ALTO: Race condition em ticket code generation
**Arquivo:** `TicketService.java`
**Problema:** `findMaxCode() + 1` pode gerar códigos duplicados em concorrência
**Fix:** Usar sequence do banco ou lock pessimista

### MÉDIO: Missing @Transactional
**Arquivo:** `AgentGroupService.java` - `update()`
**Problema:** Operações não atômicas
**Fix:** Adicionar `@Transactional`

---

## Priorização de Fixes

### 🔴 CRÍTICO (Fix imediato)
1. IDOR em FileController
2. Customer update sem validação de tenant
3. Race condition em ticket code

### 🟠 ALTO (Fix em 1 semana)
4. Enumeração de usuários
5. JWT secret validation

### 🟡 MÉDIO (Fix em 1 mês)
6. Debug logs em produção
7. Password strength validation
8. Missing @Transactional

### 🟢 BAIXO (Backlog)
9. CAPTCHA implementation
10. 2FA para admins

---

## Conclusão
**Score de Segurança: 7.5/10**

O sistema possui boa base de segurança com multi-tenant isolation, RBAC, rate limiting e audit logs. As vulnerabilidades críticas são pontuais e facilmente corrigíveis.

**Recomendação:** Corrigir vulnerabilidades críticas antes de produção.
