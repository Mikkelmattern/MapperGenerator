package app;

public enum CrudType {
    READ,
    UPDATE,
    DELETE;

    String getCrudPrefix(CrudType this){
        String s = "";
        switch (this){
            case READ -> s = "getBy";
            case UPDATE -> s = "updateBy";
            case DELETE -> s = "deleteBy";
        }
        return s;
    }
}
