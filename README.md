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
5. A file explorer dialog will open — navigate to and select the folder where you want the files generated (ideally a package folder inside `src/main/java/`)
6. Click **Open**

The tool will generate the following files:

electedFolder/

├── persistence/

│   ├── Mapper.java

│   ├── ConnectionPool.java

│   ├── Example.java

│   └── ...

├── entities/

│   ├── Example.java

│   └── ...

└── exceptions/

└── DatabaseException.java

## Notes

- Select a folder **inside** `src/main/java/` to get correct package declarations
- If you select a folder outside `src/main/java/`, the tool will warn you and generate files with simplified package names
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
