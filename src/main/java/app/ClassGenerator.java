package app;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static app.PathResolver.extractBasePackage;

public class ClassGenerator {
    private final String PUBLIC = "public ";
    private final String PRIVATE = "private ";
    private final String THIS = "this.";
    private final String INDENT = "    ";

    public void generate(List<TableDefinition> tables, Path outputDir) throws IOException {

        Path persistenceDir = outputDir.resolve("persistence");
        Path entitiesDir = outputDir.resolve("entities");
        Path exceptionDir = outputDir.resolve("exceptions");

        Files.createDirectories(persistenceDir);
        Files.createDirectories(entitiesDir);
        Files.createDirectories(exceptionDir);

        String basePackagePersistence = extractBasePackage(outputDir, "persistence");
        String basePackageEntities = extractBasePackage(outputDir, "entities");
        String basePackageException = extractBasePackage(outputDir, "exceptions");

        // Mapper Interface
        writeIfAbsent(persistenceDir.resolve("Mapper.java"), basePackagePersistence + generateMapperInterface(basePackageException));

        // ConnectionPool
        writeIfAbsent(persistenceDir.resolve("ConnectionPool.java"), basePackagePersistence + generateConnectionPool());

        // DatabaseException
        writeIfAbsent(exceptionDir.resolve("DatabaseException.java"), basePackageException + generateDatabaseException());

        // Mapper & Entity classes
        for (TableDefinition table : tables) {
            String className = toPascalCase(table.getTableName());
            writeIfAbsent(entitiesDir.resolve(className + ".java"), basePackageEntities + generateModel(table));
            writeIfAbsent(persistenceDir.resolve(className + "Mapper.java"), basePackagePersistence + generateMapper(table, basePackageException, basePackageEntities));
        }

    }

    public String toPascalCase(String tableName) {
        String[] results = tableName.split("_");
        StringBuilder name = new StringBuilder();
        for (String result : results) {
            String upperCased = Character.toUpperCase(result.charAt(0)) + result.substring(1);
            name.append(upperCased);
        }
        return name.toString();
    }

    public String toCamelCase(String columnName) {
        String[] results = columnName.split("_");
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < results.length; i++) {
            String part;
            if (i != 0) {
                part = Character.toUpperCase(results[i].charAt(0)) + results[i].substring(1);
            } else {
                part = results[i];
            }
            name.append(part);
        }
        return name.toString();
    }

    private String generateModel(TableDefinition tableDefinition) {
        String className = toPascalCase(tableDefinition.getTableName());
        List<ColumnDefinition> columns = tableDefinition.getColumns();

        return generateModelImports(tableDefinition.getColumns()) +
                PUBLIC + "class " + className + " {\n"
                + generateFields(columns)
                + generateConstructor(className, columns)
                + generateGetters(columns)
                + generateSetters(columns)
                + "}\n";
    }

    private String generateFields(List<ColumnDefinition> columns) {
        StringBuilder sb = new StringBuilder();
        for (ColumnDefinition column : columns) {
            sb
                    .append(INDENT)
                    .append(PRIVATE)
                    .append(TypeMapper.toJavaType(column.getDataType()))
                    .append(" ")
                    .append(toCamelCase(column.getColumnName()))
                    .append(";\n");
        }
        return sb.toString() + "\n";
    }

    private String generateConstructor(String className, List<ColumnDefinition> columns) {
        StringBuilder params = new StringBuilder();
        StringBuilder assignments = new StringBuilder();
        for (ColumnDefinition column : columns) {
            String javaType = TypeMapper.toJavaType(column.getDataType());
            String camelCase = toCamelCase(column.getColumnName());
            params.append(javaType).append(" ").append(camelCase).append(", ");
            assignments
                    .append(INDENT)
                    .append(INDENT)
                    .append(THIS)
                    .append(camelCase)
                    .append(" = ")
                    .append(camelCase)
                    .append(";\n");
        }
        params.setLength(params.length() - 2);
        return INDENT + PUBLIC + className + "(" + params + ") {\n" + assignments + INDENT + "}\n\n";
    }

    private String generateGetters(List<ColumnDefinition> columns) {
        StringBuilder sb = new StringBuilder();
        for (ColumnDefinition column : columns) {
            String javaType = TypeMapper.toJavaType(column.getDataType());
            String camelCase = toCamelCase(column.getColumnName());
            String pascalCase = toPascalCase(column.getColumnName());
            sb
                    .append(INDENT)
                    .append(PUBLIC)
                    .append(javaType)
                    .append(" ")
                    .append(toGetterName(column.getColumnName()))
                    .append("() {\n")
                    .append(INDENT)
                    .append(INDENT)
                    .append("return ")
                    .append(camelCase)
                    .append(";\n")
                    .append(INDENT)
                    .append("}\n\n");
        }
        return sb.toString();
    }

    public String toGetterName(String columnName) {
        return "get" + toPascalCase(columnName);
    }

    private String generateSetters(List<ColumnDefinition> columns) {
        StringBuilder sb = new StringBuilder();
        for (ColumnDefinition column : columns) {
            String javaType = TypeMapper.toJavaType(column.getDataType());
            String camelCase = toCamelCase(column.getColumnName());
            String pascalCase = toPascalCase(column.getColumnName());
            sb
                    .append(INDENT)
                    .append(PUBLIC)
                    .append("void set")
                    .append(pascalCase)
                    .append("(")
                    .append(javaType)
                    .append(" ")
                    .append(camelCase)
                    .append(") {\n")
                    .append(INDENT)
                    .append(INDENT)
                    .append(THIS)
                    .append(camelCase)
                    .append(" = ")
                    .append(camelCase)
                    .append(";\n")
                    .append(INDENT)
                    .append("}\n\n");
        }
        return sb.toString();
    }

    private String generateMapper(TableDefinition tableDefinition, String databasePath, String entititesPack) {
        String pascalCase = toPascalCase(tableDefinition.getTableName());
        String camelCase = toCamelCase(tableDefinition.getTableName());
        String mapperName = pascalCase + "Mapper";

        return generateMapperImports(entititesPack, pascalCase, databasePath, tableDefinition)
                + generateMapperClassDeclaration(mapperName, pascalCase)
                + generateMapperConstructor(mapperName)
                + generateMapperMethods(tableDefinition)
                + "}\n";
    }

    private String generateMapperImports(String entitiesPack, String pascalCase, String exceptionPack, TableDefinition tableDefinition) {

        StringBuilder fullString = new StringBuilder();

        String entitiesPath = entitiesPack.substring(8, entitiesPack.length() - 3);
        String exceptionPath = exceptionPack.substring(8, exceptionPack.length() - 3);

        String sqlImports = """
                import java.sql.Connection;
                import java.sql.PreparedStatement;
                import java.sql.ResultSet;
                import java.sql.SQLException;
                """;

        String listImports = """
                import java.util.List;
                import java.util.ArrayList;
                """;

        StringBuilder optionalImports = new StringBuilder();

        boolean hasLocalDateTime = false;
        boolean hasBigDecimal = false;

        for (ColumnDefinition column : tableDefinition.getColumns()) {
            String javaType = TypeMapper.toJavaType(column.getDataType());
            if (javaType.equals("LocalDateTime")) hasLocalDateTime = true;
            if (javaType.equals("BigDecimal")) hasBigDecimal = true;
        }

        if (hasLocalDateTime) optionalImports.append("import java.time.LocalDateTime;\n");
        if (hasBigDecimal) optionalImports.append("import java.math.BigDecimal;\n");


        fullString
                .append("import ").append(exceptionPath).append(".").append("DatabaseException").append(";\n")
                .append(sqlImports)
                .append(listImports)
                .append(optionalImports)
                .append("import ").append(entitiesPath).append(".").append(pascalCase).append(";\n\n")
        ;

        return fullString.toString();
    }

    private String generateModelImports(List<ColumnDefinition> columns) {
        StringBuilder sb = new StringBuilder();

        boolean hasLocalDateTime = false;
        boolean hasBigDecimal = false;

        for (ColumnDefinition column : columns) {
            String javaType = TypeMapper.toJavaType(column.getDataType());
            if (javaType.equals("LocalDateTime")) hasLocalDateTime = true;
            if (javaType.equals("BigDecimal")) hasBigDecimal = true;
        }

        if (hasLocalDateTime) sb.append("import java.time.LocalDateTime;\n");
        if (hasBigDecimal) sb.append("import java.math.BigDecimal;\n");

        return sb.toString();
    }

    private String generateMapperClassDeclaration(String mapperName, String pascalCase) {
        return PUBLIC + "class " + mapperName + " implements Mapper<" + pascalCase + "> {\n\n"
                + INDENT + PRIVATE + "final ConnectionPool connectionPool;\n\n";
    }

    private String generateMapperConstructor(String mapperName) {
        return INDENT + PUBLIC + mapperName + "(ConnectionPool connectionPool) {\n" + INDENT + INDENT + "this.connectionPool = connectionPool;\n" + INDENT + "}\n\n";
    }

    private String generateMapperMethods(TableDefinition tableDefinition) {
        return generateGetByIdMethod(tableDefinition)
                + generateGetAllMethod(tableDefinition)
                + generateInsertMethod(tableDefinition)
                + generateUpdateMethod(tableDefinition)
                + generateDeleteMethod(tableDefinition)
                + generateMapRow(tableDefinition);
    }

    private String generateMapperInterface(String exceptionPack) {
        String exceptionPath = exceptionPack.substring(8, exceptionPack.length() - 3);
        return "import " + exceptionPath + ".DatabaseException;\n" + """
                import java.util.List;
                
                public interface Mapper<T> {
                    T getById(int id) throws DatabaseException;
                
                    List<T> getAll() throws DatabaseException;
                
                    void insert(T entity) throws DatabaseException;
                
                    void update(T entity) throws DatabaseException;
                
                    void delete(int id) throws DatabaseException;
                }""";
    }

    private String generateGetAllMethod(TableDefinition tableDefinition) {
        StringBuilder sb = new StringBuilder();
        String pascalCase = toPascalCase(tableDefinition.getTableName());
        String camelCaseTable = toCamelCase(tableDefinition.getTableName());

        String methodSignature = INDENT + "@Override\n" + INDENT + PUBLIC + "List<" + pascalCase + "> getAll()"
                + " throws DatabaseException {\n";
        String listLine = INDENT + INDENT + "List<" + pascalCase + "> " + camelCaseTable + " = new ArrayList<>();\n";
        String sqlLine = INDENT + INDENT + "String sql = \"SELECT * FROM " + tableDefinition.getTableName() + "\";\n\n";
        String tryLine = INDENT + INDENT + "try (\n" +
                INDENT + INDENT + INDENT + INDENT + "Connection connection = connectionPool.getConnection();\n" +
                INDENT + INDENT + INDENT + INDENT + "PreparedStatement ps = connection.prepareStatement(sql);\n" +
                INDENT + INDENT + INDENT + INDENT + "ResultSet rs = ps.executeQuery()) {\n\n" +
                INDENT + INDENT + INDENT + "while (rs.next()) {\n" +
                INDENT + INDENT + INDENT + INDENT + camelCaseTable + ".add(mapRow(rs));\n\n" +
                INDENT + INDENT + INDENT + "}\n" +
                INDENT + INDENT + "} catch (SQLException e) {\n" +
                INDENT + INDENT + INDENT + "System.err.println(\"[" + pascalCase + ".getAll]\" + e.getMessage());\n" +
                INDENT + INDENT + INDENT + "throw new DatabaseException(\"Could not fetch " + tableDefinition.getTableName() + "\" + e.getMessage());\n" +
                INDENT + INDENT + "}\n" +
                INDENT + INDENT + "return " + camelCaseTable + ";\n" +
                INDENT + "}\n\n";

        sb
                .append(methodSignature)
                .append(listLine)
                .append(sqlLine)
                .append(tryLine)
        ;
        return sb.toString();
    }

    private String generateResultSetMethod(ColumnDefinition column) {
        String dataType = TypeMapper.toResultSetMethod(TypeMapper.toJavaType(column.getDataType()));
        return INDENT + "rs." + dataType + "(\"" + column.getColumnName() + "\")";
    }

    private String generateMapRow(TableDefinition table) {
        String pascalCase = toPascalCase(table.getTableName());
        StringBuilder params = new StringBuilder();

        for (ColumnDefinition column : table.getColumns()) {
            params.append(INDENT).append(INDENT).append(INDENT)
                    .append(generateResultSetMethod(column))
                    .append(",\n");
        }

        // fjern sidste komma
        params.setLength(params.length() - 2);

        return INDENT + "private " + pascalCase + " mapRow(ResultSet rs) throws SQLException {\n" +
                INDENT + INDENT + "return new " + pascalCase + "(\n" +
                params +
                "\n" + INDENT + INDENT + ");\n" +
                INDENT + "}\n";
    }

    private String generateConnectionPool() {
        return """
                import com.zaxxer.hikari.HikariConfig;
                import com.zaxxer.hikari.HikariDataSource;
                
                import java.sql.Connection;
                import java.sql.SQLException;
                import java.util.logging.Level;
                import java.util.logging.Logger;
                
                /***
                 * Singleton pattern applied to handling a Hikari ConnectionPool
                 */
                public class ConnectionPool {
                
                    public static ConnectionPool instance = null;
                    public static HikariDataSource ds = null;
                    public static String schema;
                
                    /***
                     * Empty and private constructor due to single pattern. Use getInstance methods to
                     * instantiate and get a connection pool.
                     */
                    private ConnectionPool() {
                    }
                
                    /***
                     * Getting a singleton instance of a Hikari Connection Pool with specific credentials
                     * and connection string. If an environment variable "DEPLOYED" exists then local
                     * environment variables will be inserted with user credentials and DB connection string
                     * @param user for Postgresql database user
                     * @param password for Postgresql database user
                     * @param url connection string for postgresql database. Remember to add currentSchema to string
                     * @param db database name for connection
                     * @return A ConnectionPool object
                     */
                    public static ConnectionPool getInstance(String user, String password, String url, String schema, String db) {
                        ConnectionPool.schema = schema;
                        if (instance == null) {
                            if (System.getenv("DEPLOYED") != null) {
                                ds = createHikariConnectionPool(
                                        System.getenv("JDBC_USER"),
                                        System.getenv("JDBC_PASSWORD"),
                                        System.getenv("JDBC_CONNECTION_STRING"),
                                        System.getenv("JDBC_DB"));
                            } else {
                                ds = createHikariConnectionPool(user, password, url + "?currentSchema=" + schema, db);
                            }
                            instance = new ConnectionPool();
                        }
                        return instance;
                    }
                
                    /**
                     * Getting a live connection from a Hikari Connection Pool
                     * @return a database connection to be used in sql requests
                     * @throws SQLException
                     */
                    public synchronized Connection getConnection() throws SQLException {
                        return ds.getConnection();
                    }
                
                    /***
                     * Closing a Hikari Connection Pool after use.
                     */
                    public synchronized void close() {
                        Logger.getLogger("web").log(Level.INFO, "Shutting down connection pool");
                        ds.close();
                    }
                
                    /***
                     * Configuring a Hikari DataSource ConnectionPool. Default pool size is 3.
                     * @param user for Postgresql database user
                     * @param password for Postgresql database user
                     * @param url connection string for postgresql database. Remember to add currentSchema to string
                     * @param db database name for connection
                     * @return a Hikari DataSource
                     */
                    private static HikariDataSource createHikariConnectionPool(String user, String password, String url, String db) {
                        Logger.getLogger("web").log(Level.INFO,
                                String.format("Connection Pool created for: (%s, %s, %s, %s)", user, password, url, db));
                        HikariConfig config = new HikariConfig();
                        config.setDriverClassName("org.postgresql.Driver");
                        config.setJdbcUrl(String.format(url, db));
                        config.setUsername(user);
                        config.setPassword(password);
                        config.setMaximumPoolSize(3);
                        config.setPoolName("Postgresql Pool");
                        config.addDataSourceProperty("cachePrepStmts", "true");
                        config.addDataSourceProperty("prepStmtCacheSize", "250");
                        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                        return new HikariDataSource(config);
                    }
                
                    public static void resetInstance() {
                        if (null != ds) {
                            ds.close();
                        }
                        instance = null;
                        ds = null;
                    }
                
                    public static String getSchema(){
                        return schema;
                    }
                }
                """;
    }

    private String generateDatabaseException() {
        return """
                public class DatabaseException extends Exception {
                    public DatabaseException(String message) {
                        super(message);
                    }
                }""";
    }

    private String generateInsertMethod(TableDefinition tableDefinition) {
        String pascalCase = toPascalCase(tableDefinition.getTableName());
        String camelCase = toCamelCase(tableDefinition.getTableName());

        String classPart = INDENT + "@Override\n" + INDENT +
                "public void insert(" + pascalCase + " " + camelCase + ") throws DatabaseException {\n";
        StringBuilder sqlPart = new StringBuilder(INDENT + INDENT + "String sql = \"INSERT INTO " + tableDefinition.getTableName() + " (");
        int i = 0;
        for (ColumnDefinition columnDefinition : tableDefinition.getColumns()) {
            if (!columnDefinition.isPrimaryKey()) {
                i++;
                sqlPart
                        .append(columnDefinition.getColumnName()).append(", ");
            }
        }
        sqlPart.setLength(sqlPart.length() - 2);
        sqlPart.append(") VALUES (");

        for (int j = 0; j < i; j++) {
            sqlPart.append("?, ");
        }

        sqlPart.setLength(sqlPart.length() - 2);
        sqlPart.append(")\";\n");

        String tryPart = INDENT + INDENT + "try (\n" +
                INDENT + INDENT + INDENT + INDENT + "Connection connection = connectionPool.getConnection();\n" +
                INDENT + INDENT + INDENT + INDENT + "PreparedStatement ps = connection.prepareStatement(sql)) {\n";

        StringBuilder insertPart = new StringBuilder();
        int l = 0;
        for (ColumnDefinition columnDefinition : tableDefinition.getColumns()) {
            if (!columnDefinition.isPrimaryKey()) {
                l++;
                insertPart.append(INDENT + INDENT + INDENT).append(generatePreparedStatementMethod(columnDefinition, l, camelCase));
            }
        }
        insertPart.append("\n" + INDENT + INDENT + INDENT + "ps.executeUpdate();\n");
        String catchPart = INDENT + INDENT + "} catch (SQLException e) {\n" +
                INDENT + INDENT + INDENT + "System.err.println(\"[" + pascalCase + ".insert]\" + e.getMessage());\n" +
                INDENT + INDENT + INDENT + "throw new DatabaseException(\"Could not insert into " + tableDefinition.getTableName() + "\" + e.getMessage());\n" +
                INDENT + INDENT + "}\n" +
                INDENT + "}\n\n";
        StringBuilder sb = new StringBuilder();
        return sb.append(classPart).append(sqlPart).append(tryPart).append(insertPart).append(catchPart).toString();
    }

    private String generatePreparedStatementMethod(ColumnDefinition column, int index, String entity) {
        if (column.isPrimaryKey()) return "";
        String pascalCase = toPascalCase(column.getColumnName());
        String getter = "get" + pascalCase;
        String setType = TypeMapper.toPreparedStatementMethod(TypeMapper.toJavaType(column.getDataType()));
        return "ps." + setType + "(" + index + ", " + entity + "." + getter + "());\n";
    }

    private String generateDeleteMethod(TableDefinition tableDefinition) {
        String pascalCase = toPascalCase(tableDefinition.getTableName());
        String camelCase = toCamelCase(tableDefinition.getTableName());


        String classPart = INDENT + "@Override\n" + INDENT +
                "public void delete(int id) throws DatabaseException {\n";

        String sqlPart = INDENT + INDENT + "String sql = \"DELETE FROM " + tableDefinition.getTableName() + " WHERE id = ?\";\n";

        String tryPart = INDENT + INDENT + "try (\n" +
                INDENT + INDENT + INDENT + INDENT + "Connection connection = connectionPool.getConnection();\n" +
                INDENT + INDENT + INDENT + INDENT + "PreparedStatement ps = connection.prepareStatement(sql)) {\n" +
                INDENT + INDENT + INDENT + "ps.setInt(1, id);\n" +
                INDENT + INDENT + INDENT + "ps.executeUpdate();\n" +
                INDENT + INDENT + "} catch (SQLException e) {\n" +
                INDENT + INDENT + INDENT + "System.err.println(\"[" + pascalCase + ".delete()]\" + e.getMessage());\n" +
                INDENT + INDENT + INDENT + "throw new DatabaseException(\"Could not delete from " + tableDefinition.getTableName() + "\" + e.getMessage());\n" +
                INDENT + INDENT + "}\n" +
                INDENT + "}\n\n";
        return classPart + sqlPart + tryPart;
    }

    private String generateGetByIdMethod(TableDefinition tableDefinition) {
        String pascalCase = toPascalCase(tableDefinition.getTableName());
        String camelCase = toCamelCase(tableDefinition.getTableName());

        String classPart = INDENT + "@Override\n" + INDENT +
                "public " + pascalCase + " getById(int id) throws DatabaseException {\n";

        String sqlPart = INDENT + INDENT + "String sql = \"SELECT * FROM " + tableDefinition.getTableName() + " WHERE id = ?\";\n";

        String tryPart = INDENT + INDENT + "try (\n" +
                INDENT + INDENT + INDENT + INDENT + "Connection connection = connectionPool.getConnection();\n" +
                INDENT + INDENT + INDENT + INDENT + "PreparedStatement ps = connection.prepareStatement(sql)) {\n" +
                INDENT + INDENT + INDENT + "ps.setInt(1, id);\n" +
                INDENT + INDENT + INDENT + "try (ResultSet rs = ps.executeQuery()) {\n" +
                INDENT + INDENT + INDENT + INDENT + "if (rs.next()) {\n" +
                INDENT + INDENT + INDENT + INDENT + INDENT + "return mapRow(rs);\n" +
                INDENT + INDENT + INDENT + INDENT + "}\n" +
                INDENT + INDENT + INDENT + "}\n" +
                INDENT + INDENT + "} catch (SQLException e) {\n" +
                INDENT + INDENT + INDENT + "System.err.println(\"[" + pascalCase + ".getById]\" + e.getMessage());\n" +
                INDENT + INDENT + INDENT + "throw new DatabaseException(\"Could not fetch from " + tableDefinition.getTableName() + "\" + e.getMessage());\n" +
                INDENT + INDENT + "}\n" +
                INDENT + INDENT + "return null;\n" +
                INDENT + "}\n\n";
        return classPart + sqlPart + tryPart;
    }

    private String generateUpdateMethod(TableDefinition tableDefinition) {
        String pascalCase = toPascalCase(tableDefinition.getTableName());
        String camelCase = toCamelCase(tableDefinition.getTableName());

        String classPart = INDENT + "@Override\n" + INDENT +
                "public void update(" + pascalCase + " " + camelCase + ") throws DatabaseException {\n";

        StringBuilder sqlPart = new StringBuilder(INDENT + INDENT +
                "StringBuilder sql = new StringBuilder(\"UPDATE " + tableDefinition.getTableName() + " SET \");\n");

        sqlPart.append(INDENT + INDENT + "List<Object> params = new ArrayList<>();\n\n");

        for (ColumnDefinition columnDefinition : tableDefinition.getColumns()) {
            if (columnDefinition.isPrimaryKey()) continue;
            String ifPart = INDENT + INDENT + "if(" + camelCase + "." + toGetterName(columnDefinition.getColumnName()) + "() != null) {\n"
                    + INDENT + INDENT + INDENT + "sql.append(\"" + columnDefinition.getColumnName() + " = ?, \");\n"
                    + INDENT + INDENT + INDENT + "params.add(" + camelCase + "." + toGetterName(columnDefinition.getColumnName()) + "());\n"
                    + INDENT + INDENT + "}\n\n";
            sqlPart.append(ifPart);
        }
        String exception = INDENT + INDENT + "if (params.isEmpty()) {\n"
                + INDENT + INDENT + INDENT + "System.err.println(\"[" + pascalCase + "Mapper.update()] \" + \"No params for the given \" + \"" + pascalCase + "\");\n"
                + INDENT + INDENT + INDENT + "throw new DatabaseException(\"No fields to update for " + tableDefinition.getTableName() + "\");\n"
                + INDENT + INDENT + "}\n";

        String restSql = INDENT + INDENT + "sql.setLength(sql.length() - 2);\n"
                + INDENT + INDENT + "sql.append(\" WHERE id = ?\");\n"
                + INDENT + INDENT + "params.add(" + camelCase + ".getId());\n";

        String tryPart = INDENT + INDENT + "try (\n"
                + INDENT + INDENT + INDENT + "Connection connection = connectionPool.getConnection();\n"
                + INDENT + INDENT + INDENT + "PreparedStatement ps = connection.prepareStatement(sql.toString())) {\n"
                + INDENT + INDENT + INDENT + "for (int i = 0 ; i < params.size(); i++) {\n"
                + INDENT + INDENT + INDENT + INDENT + "ps.setObject(i + 1, params.get(i));\n"
                + INDENT + INDENT + INDENT + "}\n"
                + INDENT + INDENT + INDENT + "ps.executeUpdate();\n";

        String catchPart = INDENT + INDENT + "} catch (SQLException e) {\n"
                + INDENT + INDENT + INDENT + "System.err.println(\"[" + pascalCase + "Mapper.update()]\" + e.getMessage());\n"
                + INDENT + INDENT + INDENT + "throw new DatabaseException(\"Could not update " + tableDefinition.getTableName() + "\");\n"
                + INDENT + INDENT + "}\n"
                + INDENT + "}\n\n";

        StringBuilder sb = new StringBuilder();
        sb.append(classPart)
                .append(sqlPart)
                .append(exception)
                .append(restSql)
                .append(tryPart)
                .append(catchPart);

        return sb.toString();
    }

    private void writeIfAbsent(Path filePath, String content) throws IOException {
        if (Files.exists(filePath)) {
            System.out.println("Springer over (findes allerede): " + filePath.getFileName());
            return;
        }
        Files.writeString(filePath, content);
    }
}
