package app;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SchemaReader {
    public List<TableDefinition> readSchema(ConnectionPool connectionPool) throws DatabaseException {
        Map<String, TableDefinition> tables = new LinkedHashMap<>();
        String sql = """ 
                SELECT DISTINCT ON (c.table_name, c.column_name)
                                                             c.table_name,
                                                             c.column_name,
                                                             c.data_type,
                                                             c.is_nullable,
                                                             tc.constraint_type
                                                         FROM information_schema.columns c
                                                         LEFT JOIN information_schema.key_column_usage kcu
                                                             ON c.table_name = kcu.table_name AND c.column_name = kcu.column_name
                                                         LEFT JOIN information_schema.table_constraints tc
                                                             ON kcu.constraint_name = tc.constraint_name
                                                         WHERE c.table_schema = 'public'
                                                         ORDER BY c.table_name, c.column_name,
                                                             CASE tc.constraint_type
                                                                 WHEN 'PRIMARY KEY' THEN 1
                                                                 WHEN 'FOREIGN KEY' THEN 2
                                                                 WHEN 'UNIQUE'      THEN 3
                                                                 ELSE                    4
                                                             END
                """;
        try (
                Connection connection = connectionPool.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                ResultSet resultSet = preparedStatement.executeQuery()
        ) {
            while (resultSet.next()) {
                String tableName = resultSet.getString("table_name");
                String columnName = resultSet.getString("column_name");
                String dataType = resultSet.getString("data_type");
                boolean isNullable = "YES".equals(resultSet.getString("is_nullable"));
                String constraintType = resultSet.getString("constraint_type");
                TableDefinition table = tables.computeIfAbsent(tableName, TableDefinition::new);
                table.addColumn(new ColumnDefinition(columnName, dataType, constraintType, isNullable));
            }


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return new ArrayList<>(tables.values());
    }

    ;
}
