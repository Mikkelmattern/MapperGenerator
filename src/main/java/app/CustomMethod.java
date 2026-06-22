package app;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class CustomMethod extends ClassGenerator {
    private static final String PUBLIC = "public";
    private final String methodName;
    private final TableDefinition table;
    private final List<ColumnDefinition> parameters;
    private final CrudType crudType;
    private final ReturnType returnType;
    private final String INDENT = "    ";
    private final String INDENT2 = INDENT + INDENT;
    private final String INDENT3 = INDENT2 + INDENT;

    public CustomMethod(String methodName, TableDefinition table, List<ColumnDefinition> parameters, CrudType crudType, ReturnType returnType) {
        this.methodName = methodName;
        this.table = table;
        this.parameters = parameters;
        this.crudType = crudType;
        this.returnType = returnType;
    }

    public String getMethodName() {
        return methodName;
    }

    public TableDefinition getTable() {
        return table;
    }

    public List<ColumnDefinition> getParameters() {
        return parameters;
    }

    public CrudType getCrudType() {
        return crudType;
    }

    public ReturnType getReturnType() {
        return returnType;
    }

}
