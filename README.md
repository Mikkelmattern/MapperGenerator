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

## Technology

- **Java 21** with Swing for the GUI
- **JavaFX** for the native folder picker dialog
- **HikariCP** (injected into the generated `ConnectionPool`) for connection pooling in generated code
- **PostgreSQL JDBC driver** for reading the database schema via `information_schema`
- **Maven** for dependency management

## Type mapping

The following PostgreSQL types are supported and mapped to Java types:

| PostgreSQL type | Java type |
|---|---|
| `character varying`, `text`, `char`, `USER-DEFINED` | `String` |
| `integer`, `smallint` | `Integer` |
| `bigint` | `Long` |
| `boolean` | `Boolean` |
| `numeric`, `decimal` | `BigDecimal` |
| `timestamp`, `timestamp without time zone` | `LocalDateTime` |
| `date` | `LocalDate` |
| `double precision`, `real` | `Double` |

Unsupported types fall back to `String`.

## Example

Given a `users` table with columns `id` (PK), `email` (UNIQUE), and `username`, the generator produces:

```java
public class UsersMapper implements Mapper<Users> {

    private final ConnectionPool connectionPool;

    public UsersMapper(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    @Override
    public Users getById(int id) throws DatabaseException { ... }

    @Override
    public List<Users> getAll() throws DatabaseException { ... }

    @Override
    public void insert(Users users) throws DatabaseException { ... }

    @Override
    public void update(Users users) throws DatabaseException { ... }

    @Override
    public void delete(int id) throws DatabaseException { ... }

    private Users mapRow(ResultSet rs) throws SQLException {
        return new Users(
            rs.getString("email"),
            rs.getInt("id"),
            rs.getString("username")
        );
    }
}
```

With a custom READ method on `email`, the generator appends:

```java
public Users getUsersByEmail(String email) throws DatabaseException {
    String sql = "SELECT * FROM users WHERE email = ?";
    try (
            Connection connection = connectionPool.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setString(1, email);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return mapRow(rs);
            }
        }
    } catch (SQLException e) {
        System.err.println("[Users.getUsersByEmail]" + e.getMessage());
        throw new DatabaseException("Could not fetch from users");
    }
    return null;
}
```

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

## Planned features

- **Unique column indicator** — mark unique columns visually in the parameter list so it is clear which selections will produce a single object vs. a list
- **Remove method** — a remove button in the custom methods dialog to undo a queued method before generating
- **Validation** — warn if the user tries to add a method without selecting any parameters, or if the method name is already taken
- **Update existing mappers** — instead of skipping files that already exist, offer to append custom methods to them without touching the rest of the file
- **Multiple schemas** — let the user select which PostgreSQL schemas to read, instead of always defaulting to `public`
- **JOIN methods** — generate methods that join two tables based on detected foreign key relationships (e.g. `getOrderWithCustomer(int orderId)`)
- **Configuration file** — save connection details to a local `.properties` file so they do not need to be re-entered on every run

## Limitations

- **Custom methods are only added to new files.** Because existing files are never overwritten, custom methods are not appended to mappers that already exist on disk. To add custom methods to an existing mapper, delete the file and re-run the generator.
- **UNIQUE constraint detection depends on `information_schema`.** Some database setups or non-standard constraints may not be visible through `information_schema`, in which case a UNIQUE column may be treated as non-unique and return `List<T>` instead of `T`. You can always rename the method manually in the dialog or edit the generated code.
- **Only the `public` schema is read.** Tables in other schemas are not included.
- **Unsupported PostgreSQL types fall back to `String`.** If your schema uses a type not in the type mapping table above, the generated field will be typed as `String` and may require manual adjustment.
- **The generated `update` method requires wrapper types.** Primitive fields (`int`, `boolean`) cannot be `null`, so the null-check logic in `update` will not work for them. All entity fields are generated as wrapper types (`Integer`, `Boolean`) to support this pattern.
