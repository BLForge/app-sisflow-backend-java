# Migration Troubleshooting Guide

## Current Issues Fixed

### 1. Missing Auth Schema
- **Problem**: V020 migration was trying to create functions in `auth` schema that didn't exist
- **Solution**: V021 migration creates the `auth` schema first and uses safer function definitions

### 2. Missing Visibility Columns
- **Problem**: V015 and V016 were marked as executed but never actually ran
- **Solution**: V021 migration safely adds visibility columns using `IF NOT EXISTS` logic

### 3. RLS Function Issues
- **Problem**: Functions were using `auth.` prefix without the schema existing
- **Solution**: V021 creates functions in public schema with proper error handling

## How to Verify Migrations Are Working

### Option 1: Run the Verification Script
```bash
# Connect to your PostgreSQL database and run:
psql $DATABASE_URL -f check-migrations.sql
```

### Option 2: Manual Checks

1. **Check Flyway History**:
```sql
SELECT version, description, installed_on, success 
FROM flyway_schema_history 
ORDER BY installed_rank DESC;
```

2. **Check Visibility Columns**:
```sql
\d tickets
\d ticket_interactions
```

3. **Check RLS Functions**:
```sql
\df get_current_user_id
\df is_admin_or_above
```

## Railway Deployment Notes

### Flyway Configuration
The application.properties has been updated with Railway-friendly settings:
- `spring.flyway.clean-disabled=true` - Prevents accidental data loss
- `spring.flyway.ignore-future-migrations=true` - Handles version conflicts
- `spring.flyway.baseline-on-migrate=true` - Works with existing databases

### Environment Variables Required
Make sure these are set in Railway:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME` 
- `SPRING_DATASOURCE_PASSWORD`

## Expected Migration Order

1. **V000**: Creates all base tables (including visibility columns)
2. **V001-V017**: Various table modifications
3. **V018**: Safely adds visibility columns if missing
4. **V020**: Original RLS implementation (may fail due to auth schema)
5. **V021**: Fixed RLS implementation with proper schema creation

## If Migrations Still Don't Run

### Check Railway Logs
Look for Flyway messages in your Railway deployment logs:
```
INFO  FlywayAutoConfiguration - Flyway enabled
INFO  Flyway - Database: jdbc:postgresql://...
INFO  Flyway - Successfully validated X migrations
INFO  Flyway - Current version of schema "public": X
```

### Force Migration Repair (Last Resort)
If migrations are completely broken:
```sql
-- Connect to your database and run:
DELETE FROM flyway_schema_history WHERE version IN ('015', '016', '020');
-- Then redeploy to let V021 run
```

### Manual Column Addition (Emergency Fix)
If you need the visibility columns immediately:
```sql
-- Add visibility columns manually
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS visibility VARCHAR(50) DEFAULT 'PROJECT' NOT NULL;
ALTER TABLE ticket_interactions ADD COLUMN IF NOT EXISTS visibility VARCHAR(50) DEFAULT 'PROJECT' NOT NULL;

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_tickets_visibility ON tickets(visibility);
CREATE INDEX IF NOT EXISTS idx_ticket_interactions_visibility ON ticket_interactions(visibility);
```

## Testing RLS After Migration

1. **Set user context**:
```sql
SET app.current_user_id = 'your-user-uuid-here';
```

2. **Test function**:
```sql
SELECT get_current_user_id();
SELECT get_current_user_hierarchy_level();
```

3. **Test policies**:
```sql
SELECT * FROM tickets; -- Should only show tickets you can access
```