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
5. A folder dialog will open — select your **project folder** (or any folder inside `src/main/java/`)
6. Click **Select Folder**

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

If the selected folder is not already inside `src/main/java/`, the tool automatically creates `src/main/java/app/` in the selected folder and generates the files there.

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
