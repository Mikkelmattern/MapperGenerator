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

    public String create() {
        return createMethodSignature();
    }

    private String createMethodSignature() {
        StringBuilder signature = new StringBuilder();
        String pascalCase = toPascalCase(table.getTableName());
        signature.append(INDENT).append("public ");


        switch (returnType) {
            case LIST -> signature.append("List<")
                    .append(pascalCase)
                    .append("> ");

            case OBJECT -> signature.append(pascalCase)
                    .append(" ");

            case VOID -> signature.append("void ");

        }

        signature.append(methodName)
                .append(getMethodSignatureParams());
        return signature.toString();
    }


    private String transformParamsToMethodSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (ColumnDefinition parameter : parameters) {
            sb.append(TypeMapper.toJavaType(parameter.getDataType()))
                    .append(toPascalCase(parameter.getColumnName()))
                    .append(", ");
        }
        sb.setLength(sb.length() - 2);
        sb.append(")");
        return sb.toString();
    }

    private String getMethodSignatureParams() {
        StringBuilder sb = new StringBuilder();

        sb.append("(")
                .append(toPascalCase(table.getTableName()))
                .append(" ")
                .append(toCamelCase(table.getTableName()))
                .append(", ConnectionPool connectionPool) throws DatabaseException {\n")
                .append(createSqlString())
        ;
        return sb.toString();
    }

    private String transformParamsToSql() {
        StringBuilder sb = new StringBuilder();
        for (ColumnDefinition param : parameters) {
            sb.append(param.getColumnName()).append(" = ?, ");
        }
        sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    private String createSqlString() {
        StringBuilder sql = new StringBuilder(INDENT2);
        if (returnType == ReturnType.LIST) {
            String s = String.format("List<%s> %s = new ArrayList<>();\n\n" + INDENT2, toPascalCase(table.getTableName()), toCamelCase(table.getTableName() + "List"));
            sql.append(s);
        }
        switch (crudType) {
            case READ -> {
                sql.append("String sql = \"SELECT * FROM ")
                        .append(table.getTableName())
                        .append(" WHERE ")
                        .append(transformParamsToSql())
                        .append("\");\n")
                        .append(tryCatchPart(betweenRead(), "An error occurred while trying to read from " + table.getTableName()))
                ;
            }
            case UPDATE -> {
                sql.append("StringBuilder sql = new StringBuilder(\"UPDATE ")
                        .append(table.getTableName())
                        .append(" SET \");\n")
                        .append(ifAndTryCatchForUpdate())
                ;
            }
            case DELETE -> {
                sql.append("String sql = \"DELETE FROM ")
                        .append(table.getTableName())
                        .append(" WHERE ")
                        .append(transformParamsToSql())
                        .append("\");\n")
                ;
            }
        }
        return sql.toString();
    }

    private String ifAndTryCatchForUpdate() {

        StringBuilder sb = new StringBuilder(INDENT2 + "List<Object> params = new ArrayList<>();\n\n");
        for (ColumnDefinition column : table.getColumns()) {
            if (column.isPrimaryKey()) continue;
            sb.append(INDENT2)
                    .append("if (")
                    .append(toCamelCase(table.getTableName()))
                    .append(".")
                    .append(toGetterName(toPascalCase(column.getColumnName())))
                    .append("() != null) {\n")
                    .append(INDENT3)
                    .append("sql.append(\"")
                    .append(column.getColumnName())
                    .append(" = ?, \");\n")
                    .append(INDENT3)
                    .append("params.add(")
                    .append(toCamelCase(table.getTableName()))
                    .append(".")
                    .append(toGetterName(toPascalCase(column.getColumnName())))
                    .append("());\n")
                    .append(INDENT2)
                    .append("}\n")
                    .append(tryCatchForUpdate())
            ;


        }
        return sb.toString();
    }

    private String tryCatchForUpdate() {
        String privateKey = table.getColumns().stream().filter(ColumnDefinition::isPrimaryKey)
                .findFirst().map(ColumnDefinition::getColumnName).orElse(null);
        String classVar = toCamelCase(table.getTableName());
        String methodVar = toGetterName(privateKey);
        return String.format(INDENT2 + """
                        if (params.isEmpty()) {
                            throw new DatabaseException("No fields to update for material with id " + material.getId());
                        }
                
                        sql.setLength(sql.length() - 2);
                        sql.append(" WHERE %s = ?");
                        params.add(%s.%s());
                
                        try (
                                Connection connection = connectionPool.getConnection();
                                PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())
                        ) {
                            for (int i = 0; i < params.size(); i++) {
                                preparedStatement.setObject(i + 1, params.get(i));
                            }
                            preparedStatement.executeUpdate();
                
                        } catch (SQLException e) {
                            System.err.println("[MaterialsMapper.updateMaterialInfo] " + e.getMessage());
                            throw new DatabaseException("Could not update material with id " + material.getId());
                        }
                }""", privateKey, classVar, methodVar);
    }

    private String generateGetMethod() {

        StringBuilder sb = new StringBuilder();
        String pascalCase = toPascalCase(table.getTableName());
        String camelCaseTable = toCamelCase(table.getTableName());

        String methodSignature = createMethodSignature();
        if (returnType == ReturnType.LIST) {
            String listLine = INDENT + INDENT + "List<" + pascalCase + "> " + camelCaseTable + " = new ArrayList<>();\n";
        }

        String sqlLine = ;

        String tryLine = INDENT + INDENT + "try (\n" +
                INDENT + INDENT + INDENT + INDENT + "Connection connection = connectionPool.getConnection();\n" +
                INDENT + INDENT + INDENT + INDENT + "PreparedStatement ps = connection.prepareStatement(sql);\n" +
                INDENT + INDENT + INDENT + INDENT + "ResultSet rs = ps.executeQuery()) {\n\n" +
                INDENT + INDENT + INDENT + "while (rs.next()) {\n" +
                INDENT + INDENT + INDENT + INDENT + camelCaseTable + ".add(mapRow(rs));\n\n" +
                INDENT + INDENT + INDENT + "}\n" +
                INDENT + INDENT + "} catch (SQLException e) {\n" +
                INDENT + INDENT + INDENT + "System.err.println(\"[" + pascalCase + ".getAll]\" + e.getMessage());\n" +
                INDENT + INDENT + INDENT + "throw new DatabaseException(\"Could not fetch " + table.getTableName() + "\" + e.getMessage());\n" +
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

    private String tryCatchPart(String betweenTryAndCatch, String errorMessage) {
        return String.format("""
                try (
                    Connection connection = connectionPool.getConnection();
                    PreparedStatement preparedStatement = connection.prepareStatement(sql)
                ) {
                 %s
                } catch (SQLException e) {
                    System.err.println("[%s] " + e.getMessage());
                    throw new DatabaseException("%s");
                }
            }""", betweenTryAndCatch, toPascalCase(table.getTableName()) + "." + methodName, errorMessage);
    }

    private String betweenRead() {
        StringBuilder pStatements = new StringBuilder(INDENT3);
        int i = 0;
        for (ColumnDefinition param : parameters) {
            pStatements.append("preparedStatement.")
                    .append(TypeMapper.toResultSetMethod(TypeMapper.toJavaType(param.getDataType())))
                    .append("(")
                    .append(++i)
                    .append(", ").append(toPascalCase(table.getTableName())).append(".").append(toGetterName(param.getColumnName()))
                    .append(");\n")
            ;
        }
        pStatements.append(INDENT3).append("ResultSet resultSet = preparedStatement.executeQuery();\n\n")
                .append(whilePartOfRead())
        ;
        return pStatements.toString();
    }

    private String whilePartOfRead() {
        StringBuilder stringBuilder = new StringBuilder(INDENT3 + "while(resultSet.next()) {\n");
        List<String> strings = new ArrayList<>();
        for (ColumnDefinition column : table.getColumns()) {
            stringBuilder.append(INDENT3 + INDENT)
                    .append(TypeMapper.toJavaType(column.getDataType()))
                    .append(" ").append(toCamelCase(column.getColumnName())).append(" = resultSet.")
                    .append(TypeMapper.toResultSetMethod(TypeMapper.toJavaType(column.getDataType())))
                    .append("(\"")
                    .append(column.getColumnName())
                    .append("\");\n");
            strings.add(toCamelCase(column.getColumnName()));
        }
        StringBuilder sb = new StringBuilder();
        strings.forEach(string -> sb.append(string).append(", "));
        sb.setLength(sb.length() - 2);

        String s = String.format(INDENT3 + INDENT + "%s %s = new %s(%s);\n\n", toPascalCase(table.getTableName()), toCamelCase(table.getTableName()), toPascalCase(table.getTableName()), sb.toString());
        String t = String.format(INDENT3 + INDENT + "%s.add(%s);\n" + INDENT3 + "}\n\nreturn %s", toCamelCase(table.getTableName()), table.getTableName() + "List", table.getTableName() + "List");
        stringBuilder.append(s).append(t);

    }

}
