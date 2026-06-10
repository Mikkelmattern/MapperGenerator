package app;

public class ColumnDefinition {
    private final String columnName;
    private final String dataType;
    private final boolean isPrimaryKey;
    private final boolean isForeignKey;
    private final boolean isNullable;

    public ColumnDefinition(String columnName, String dataType, String constraintType, boolean isNullable) {
        this.columnName = columnName;
        this.dataType = dataType;
        this.isPrimaryKey = "PRIMARY KEY".equals(constraintType);
        this.isForeignKey = "FOREIGN KEY".equals(constraintType);
        this.isNullable = isNullable;
    }

    // Getters
    public String getColumnName() {
        return columnName;
    }

    public String getDataType() {
        return dataType;
    }

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public boolean isForeignKey() {
        return isForeignKey;
    }

    public boolean isNullable() {
        return isNullable;
    }
}
