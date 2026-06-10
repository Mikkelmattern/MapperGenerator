package app;

public class TypeMapper {
    public static String toJavaType(String sqlType) {
        return switch (sqlType) {
            case "integer", "serial" -> "Integer";
            case "character varying", "text" -> "String";
            case "boolean" -> "Boolean";
            case "timestamp" -> "LocalDateTime";
            case "numeric" -> "BigDecimal";
            case "USER-DEFINED" -> "String";
            default -> "String"; // fallback
        };
    }

    public static String toResultSetMethod(String javaType) {
        return switch (javaType) {
            case "Integer", "int" -> "getInt";
            case "String" -> "getString";
            case "Boolean", "boolean" -> "getBoolean";
            case "LocalDateTime" -> "getTimestamp";
            case "BigDecimal" -> "getBigDecimal";
            default -> "getString";
        };
    }

    public static String toPreparedStatementMethod(String javaType) {
        return switch (javaType) {
            case "Integer", "int" -> "setInt";
            case "String" -> "setString";
            case "Boolean", "boolean" -> "setBoolean";
            case "LocalDateTime" -> "setTimestamp";
            case "BigDecimal" -> "setBigDecimal";
            default -> "setString";
        };
    }
}
