# Historical Order Query Guide (English)

## Overview

The system supports querying historical orders/transactions from the `sales_data` table. You can search by:
- **Date range** (start date, end date)
- **Buyer information** (buyer name, buyer code)
- **Transaction number** (TXNo)
- **Product information** (item code, item name)
- **Combined conditions** (date + buyer + product)

## Supported Query Types

### 1. Query by Date Range

**Examples:**
```
show orders from 2024
orders in 2023
transactions from January 2024 to March 2024
sales data from 2024-01-01 to 2024-12-31
historical orders last year
orders this year
```

### 2. Query by Buyer Name

**Examples:**
```
orders from AIR LIQUIDE SINGAPORE PRIVATE LIMITED
show transactions for AIR LIQUIDE
buyer AIR LIQUIDE orders
purchases by AIR LIQUIDE SINGAPORE PRIVATE LIMITED
```

### 3. Query by Buyer Code

**Examples:**
```
orders for buyer code ABC123
transactions with buyer code XYZ
show orders buyer code 12345
```

### 4. Query by Transaction Number (TXNo)

**Examples:**
```
show transaction TX12345
order number TX12345
find transaction TX12345
```

### 5. Combined Queries

**Date + Buyer:**
```
orders from AIR LIQUIDE in 2024
transactions for AIR LIQUIDE from January to March 2024
show AIR LIQUIDE orders last year
```

**Date + Product:**
```
safety equipment orders in 2024
steel pipe transactions from 2023
show safety helmet orders from last year
```

**Buyer + Product:**
```
AIR LIQUIDE safety equipment orders
show AIR LIQUIDE steel pipe purchases
```

**Date + Buyer + Product:**
```
AIR LIQUIDE safety equipment orders in 2024
show AIR LIQUIDE steel pipe transactions from 2023
```

### 6. Query by Item Code with History

**Examples:**
```
TI00040
show history for TI00040
historical transactions for item code TI00040
TI00040 all orders
```

This will return:
- All historical transactions for that item code
- Price statistics (min, max, average)
- Date range (first transaction, last transaction)

## Query Format Tips

### Date Formats Supported:
- "2024" - entire year
- "2024-01" - January 2024
- "2024-01-15" - specific date
- "last year" - previous year
- "this year" - current year
- "January 2024" - month and year
- "from 2024-01-01 to 2024-12-31" - date range

### Buyer Name Formats:
- Full company name: "AIR LIQUIDE SINGAPORE PRIVATE LIMITED"
- Partial name: "AIR LIQUIDE"
- Company name with keywords: "LIMITED", "PRIVATE", "COMPANY", "CORP"

### Transaction Number:
- Format: "TX" followed by numbers/letters
- Example: "TX12345", "TX-2024-001"

## Response Format

When you query historical orders, the system will return:

1. **Table Data** with columns:
   - id
   - Item Code
   - Item Name
   - Date (TXDate)
   - Price (TXP1)
   - Quantity (TXQty)
   - Buyer Code
   - Buyer Name
   - Category
   - Brand
   - Function
   - Model
   - Material
   - etc.

2. **AI Response** explaining:
   - Number of orders found
   - Date range
   - Key statistics (if applicable)
   - Summary of results

## Example Queries

### Simple Date Query:
```
show orders from 2024
```

### Buyer + Date:
```
AIR LIQUIDE orders in 2024
```

### Product + Date:
```
safety equipment orders from last year
```

### Complete Example:
```
show all safety equipment orders from AIR LIQUIDE in 2024
```

## Notes

- All queries should be in **English**
- The system uses AI to understand your query and extract search criteria
- Results are sorted by transaction date (newest first)
- If no results found, the AI will provide suggestions

