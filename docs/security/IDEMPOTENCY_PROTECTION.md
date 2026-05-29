# Idempotency Protection

**Date:** May 10, 2026  
**Status:** Implemented

---

## Overview

Idempotency protection prevents duplicate processing of requests when users accidentally click submit buttons multiple times or when network issues cause request retries.

## How It Works

### Frontend
Every POST/PUT/PATCH request automatically includes a unique `Idempotency-Key` header:

```typescript
// Automatically added in apiFetch()
const method = options.method?.toUpperCase();
if (method === "POST" || method === "PUT" || method === "PATCH") {
  headers["Idempotency-Key"] = crypto.randomUUID();
}
```

### Backend
The `IdempotencyFilter` tracks requests by user + idempotency key:

```java
String key = "idempotency:" + userId + ":" + idempotencyKey;
var bucket = rateLimitService.resolveBucket(key, 1, Duration.ofMinutes(5));

if (!bucket.tryConsume(1)) {
    // Duplicate request - reject with 409 CONFLICT
    return DUPLICATE_REQUEST error;
}
```

## Behavior

**First Request:**
- Idempotency-Key: `a1b2c3d4-5678-90ab-cdef-1234567890ab`
- Status: 200 OK (processed)

**Duplicate Request (within 5 minutes):**
- Same Idempotency-Key
- Status: 409 CONFLICT
- Error: `DUPLICATE_REQUEST`
- Message: "Requisição duplicada. Aguarde o processamento."

**After 5 Minutes:**
- Same Idempotency-Key can be reused
- Allows legitimate retries after timeout

## Protection Layers

1. **Frontend Button Disable** — Immediate UI feedback
2. **React Query** — Prevents concurrent mutations
3. **Idempotency Filter** — Backend rejection of duplicates
4. **Database Transactions** — ACID guarantees
5. **Unique Constraints** — Final safety net

## Testing

```bash
# Get idempotency key
KEY=$(uuidgen)

# First request - succeeds
curl -X POST http://localhost:8080/tickets \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: $KEY" \
  -d '{"title":"Test","customerId":"...","slaId":"..."}'

# Duplicate request - rejected
curl -X POST http://localhost:8080/tickets \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: $KEY" \
  -d '{"title":"Test","customerId":"...","slaId":"..."}'
# Returns: 409 CONFLICT - DUPLICATE_REQUEST
```

## Configuration

**Window Duration:** 5 minutes (configurable in `IdempotencyFilter.java`)

```java
// Change from 5 minutes to 10 minutes
var bucket = rateLimitService.resolveBucket(key, 1, Duration.ofMinutes(10));
```

## Notes

- Idempotency-Key is optional — requests without it are not checked
- Each user has separate idempotency tracking
- Anonymous requests use "anonymous" as user ID
- Keys expire after 5 minutes to prevent memory bloat

---

**Summary:** Double-submit is now impossible. If a user clicks twice, the second request is rejected with a clear error message.
