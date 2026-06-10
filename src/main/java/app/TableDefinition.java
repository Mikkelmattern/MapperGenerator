package app;

import java.util.ArrayList;
import java.util.List;

public class TableDefinition {
    private final String tableName;
    private final List<ColumnDefinition> columns = new ArrayList<>();

    public TableDefinition(String tableName) {
        this.tableName = tableName;
    }


    public String getTableName() {
        return tableName;
    }

    public List<ColumnDefinition> getColumns() {
        return columns;
    }

    public void addColumn(ColumnDefinition column) {
        columns.add(column);
    }
}
