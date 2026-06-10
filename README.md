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
