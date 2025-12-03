# Database Initial Setup Guide

## Prerequisites

Before running Flyway migrations, you need to create the database manually.

## Step 1: Create Database

Connect to MySQL and create the database:

```sql
CREATE DATABASE IF NOT EXISTS ecoschema 
    DEFAULT CHARACTER SET utf8mb4 
    DEFAULT COLLATE utf8mb4_unicode_ci;
```

Or via command line:

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS ecoschema DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;"
```

## Step 2: Configure Database Connection

Update `src/main/resources/application.yml` with your database credentials:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ecoschema?useSSL=false&serverTimezone=UTC&characterEncoding=utf8&useUnicode=true
    username: your_username
    password: your_password
```

## Step 3: Run Application

Start the Spring Boot application. Flyway will automatically:

1. Detect the database connection
2. Create the `flyway_schema_history` table (if needed)
3. Execute all migration scripts in order (V1, V2, V3, ...)
4. Track executed migrations

## Step 4: Verify

Check that all tables were created:

```sql
USE ecoschema;
SHOW TABLES;
```

You should see:
- `flyway_schema_history` (Flyway tracking table)
- `users`
- `sellers`
- `products`
- `cart_items`
- `orders`
- `order_items`
- `wishlist_items`
- `product_hierarchy_mapping`
- `sales_data`

Check migration history:

```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

## Troubleshooting

### Database doesn't exist

If you see an error about the database not existing:
1. Create the database manually (see Step 1)
2. Restart the application

### Migration fails

If a migration fails:
1. Check the error message in application logs
2. Fix the issue in the database manually if needed
3. Check `flyway_schema_history` table to see which migrations succeeded

### Reset everything (Development only)

⚠️ **Warning**: This will delete all data!

```sql
DROP DATABASE IF EXISTS ecoschema;
CREATE DATABASE ecoschema DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
```

Then restart the application to re-run all migrations.

