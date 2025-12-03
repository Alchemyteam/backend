# Flyway Database Migration Guide

This project uses **Flyway** for database version control and migration management.

## What is Flyway?

Flyway is a database migration tool that allows you to:
- Version control your database schema
- Automatically apply migrations when the application starts
- Track which migrations have been executed
- Collaborate with team members without manual database synchronization

## How It Works

1. **Migration Scripts**: All database changes are stored as SQL files in `src/main/resources/db/migration/`
2. **Versioning**: Scripts are named with version numbers: `V{version}__{description}.sql`
3. **Automatic Execution**: When the application starts, Flyway automatically runs any new migration scripts
4. **Tracking**: Flyway creates a `flyway_schema_history` table to track executed migrations

## Migration Script Naming Convention

```
V{version}__{description}.sql
```

Examples:
- `V1__create_database_and_users_table.sql`
- `V2__create_sellers_table.sql`
- `V9__create_sales_data_table.sql`

**Important Rules:**
- Version number must be unique and sequential
- Use double underscore `__` to separate version and description
- Description should be descriptive but concise
- Scripts are executed in version order

## Current Migration Scripts

| Version | Description | File |
|---------|-------------|------|
| V1 | Create users table | `V1__create_database_and_users_table.sql` |
| V2 | Create sellers table | `V2__create_sellers_table.sql` |
| V3 | Create products table | `V3__create_products_table.sql` |
| V4 | Create cart items table | `V4__create_cart_items_table.sql` |
| V5 | Create orders table | `V5__create_orders_table.sql` |
| V6 | Create order items table | `V6__create_order_items_table.sql` |
| V7 | Create wishlist items table | `V7__create_wishlist_items_table.sql` |
| V8 | Create product hierarchy mapping table | `V8__create_product_hierarchy_mapping_table.sql` |
| V9 | Create sales data table | `V9__create_sales_data_table.sql` |

## How to Add a New Migration

### Step 1: Create the Migration Script

Create a new SQL file in `src/main/resources/db/migration/` with the next version number:

```sql
-- V10__add_new_column_to_products.sql
ALTER TABLE products 
ADD COLUMN new_field VARCHAR(255) DEFAULT NULL COMMENT 'New field description';
```

### Step 2: Commit to Git

```bash
git add src/main/resources/db/migration/V10__add_new_column_to_products.sql
git commit -m "Add new field to products table"
git push
```

### Step 3: Team Members Update

When team members pull your changes and start the application:
1. Flyway detects the new migration script
2. Automatically executes it
3. Updates the `flyway_schema_history` table
4. Their database is now synchronized with yours

**No manual SQL execution needed!**

## Initial Setup

**Important**: Before running migrations, you must create the database manually. See `database/INITIAL_SETUP.md` for details.

## Configuration

Flyway is configured in `application.yml`:

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true  # Create baseline for existing databases
    validate-on-migrate: true  # Validate migrations on startup
    clean-disabled: true       # Disable clean command for safety
```

## Best Practices

### ✅ DO:
- Always use `CREATE TABLE IF NOT EXISTS` or `ALTER TABLE` (not `DROP TABLE`)
- Test migrations on a development database first
- Use descriptive migration names
- Keep migrations small and focused
- Commit migration scripts to Git immediately after creating them

### ❌ DON'T:
- Modify existing migration scripts after they've been executed
- Use `DROP TABLE` in migrations (use separate cleanup scripts if needed)
- Skip version numbers
- Create migrations that depend on data (use separate data migration scripts)

## Troubleshooting

### Migration Failed

If a migration fails:
1. Check the error message in the application logs
2. Fix the SQL script
3. If the migration was partially applied, you may need to manually fix the database
4. Create a new migration script to fix the issue

### Reset Database (Development Only)

⚠️ **Warning**: Only use in development!

```sql
-- Drop all tables
DROP TABLE IF EXISTS sales_data;
DROP TABLE IF EXISTS product_hierarchy_mapping;
DROP TABLE IF EXISTS wishlist_items;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS sellers;
DROP TABLE IF EXISTS users;

-- Clear Flyway history
DROP TABLE IF EXISTS flyway_schema_history;
```

Then restart the application - Flyway will re-run all migrations.

## Checking Migration Status

You can check which migrations have been executed by querying the `flyway_schema_history` table:

```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

## Benefits

1. **Version Control**: Database schema is versioned in Git
2. **Automation**: No manual SQL execution needed
3. **Consistency**: All team members have the same database structure
4. **History**: Track all database changes over time
5. **Rollback**: Can create rollback scripts if needed

## Resources

- [Flyway Documentation](https://flywaydb.org/documentation/)
- [Flyway with Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.flyway)

