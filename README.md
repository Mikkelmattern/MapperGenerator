# MapperGenerator

A Java tool that automatically generates entity classes and mapper classes based on a PostgreSQL database schema.

## Requirements

- Java 21+
- Maven
- A running PostgreSQL database

## How to use

1. Clone the repository and open it in IntelliJ IDEA
2. Run the `Main` class
3. Fill in the database connection details:
   - **User** — your PostgreSQL username
   - **Password** — your PostgreSQL password
   - **URL** — connection string, e.g. `jdbc:postgresql://localhost:5432/%s`
   - **Schema** — typically `public`
   - **Database** — your database name
4. Click **Submit**
5. A custom methods dialog opens — add any extra mapper methods you need (optional), then click **Ok**
6. A folder dialog will open — select your **project folder** (or any folder inside `src/main/java/`)
7. Click **Select Folder**

## Environment variables (optional)

The connection fields can be pre-filled automatically by setting the following environment variables:

| Variable | Description |
|---|---|
| `JDBC_USER` | PostgreSQL username |
| `JDBC_PASSWORD` | PostgreSQL password |
| `JDBC_CONNECTION_STRING` | Connection string, e.g. `jdbc:postgresql://localhost:5432/%s` |
| `JDBC_DB` | Database name |

In IntelliJ, these can be set under **Run → Edit Configurations → Environment variables**.

## Output

If the selected folder is not already inside `src/main/java/`, the tool automatically finds or creates `src/main/java/app/` in the selected folder and generates the files there.

```
src/main/java/app/
├── persistence/
│   ├── Mapper.java
│   ├── ConnectionPool.java
│   ├── ExampleMapper.java
│   └── ...
├── entities/
│   ├── Example.java
│   └── ...
└── exceptions/
    └── DatabaseException.java
```

Package declarations are derived automatically from the folder structure, so the generated files compile without manual adjustments.

## Existing files are never overwritten

If a generated file already exists, it is **skipped**. This means you can safely re-run the generator after adding new tables — your hand-written changes to existing mappers are preserved.

To regenerate a file from scratch (e.g. after changing a table's columns), delete the file and run the generator again.

## Notes

- Primary keys are automatically excluded from `insert` methods
- The `update` method only updates fields that are not `null`
- `USER-DEFINED` types (PostgreSQL enums) are mapped to `String`

## Generated mapper methods

Each mapper implements the `Mapper<T>` interface with the following methods:

| Method | Description |
|---|---|
| `getById(int id)` | Fetch a single entity by primary key |
| `getAll()` | Fetch all entities |
| `insert(T entity)` | Insert a new entity (skips primary key) |
| `update(T entity)` | Update only non-null fields |
| `delete(int id)` | Delete by primary key |

## Custom methods

After connecting, a dialog lets you define extra methods to append to any mapper. For each method:

1. **Table** — pick which table the method belongs to
2. **Type** — choose the operation: `READ`, `UPDATE`, or `DELETE`
3. **Parameters** — select one or more columns; these become the `WHERE` clause conditions
4. **Method name** — auto-generated from the type, table, and selected columns (e.g. `getOrderByUserId`); you can rename it manually

Click **Add Method** to queue the method, then repeat for any others. Click **Ok** when done.

### Return type inference

The return type is inferred automatically — you do not select it manually:

| Situation | Return type |
|---|---|
| READ with a primary key or unique column as parameter | `T` (single object) |
| READ with any other column(s) | `List<T>` |
| UPDATE or DELETE | `void` |

### Generated SQL

| Type | SQL pattern |
|---|---|
| READ | `SELECT * FROM table WHERE col = ?` |
| UPDATE | Dynamic `UPDATE table SET ... WHERE col = ?` (same null-check logic as the standard `update`) |
| DELETE | `DELETE FROM table WHERE col = ?` |

Multiple parameters are joined with `AND`.
