package com.bonsaimarket.backend.support;

public final class H2Json {
    private static final tools.jackson.databind.ObjectMapper JSON=new tools.jackson.databind.ObjectMapper();
    public static boolean JSON_VALID(String value){try{JSON.readTree(value);return value!=null;}catch(Exception e){return false;}}
    public static String JSON_EXTRACT(String value,String path){
        if(value==null)return null;
        var node=JSON.readTree(value);
        for(String key:path.replaceFirst("^\\$[.]","").split("[.]")){node=node.get(key);if(node==null)return null;}
        return node.toString();
    }
    public static String JSON_UNQUOTE(String value){if(value==null)return null;var node=JSON.readTree(value);return node.isString()?node.asString():node.toString();}
    public static String JSON_TYPE(String value){if(value==null)return null;var node=JSON.readTree(value);return node.isString()?"STRING":node.isIntegralNumber()?"INTEGER":node.isNumber()?"DOUBLE":node.isBoolean()?"BOOLEAN":node.isNull()?"NULL":node.isArray()?"ARRAY":"OBJECT";}
}
