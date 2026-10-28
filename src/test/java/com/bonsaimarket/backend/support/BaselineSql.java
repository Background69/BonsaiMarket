package com.bonsaimarket.backend.support;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Connection;
import java.util.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/** Đọc duy nhất baseline; điều chỉnh H2 chỉ dùng cho kiểm thử cô lập. */
public final class BaselineSql {
    public static final Path SCHEMA=Path.of("src/main/resources/db/migration/V1__initial_schema.sql");
    public static final Path SEED=Path.of("src/main/resources/db/dev/R__dev_seed.sql");
    private BaselineSql() {}
    public static String tables(String... names) throws Exception {
        var wanted=Set.of(names);var result=new StringBuilder();
        for(String part:Files.readString(SCHEMA).split(";")) {
            String sql=part.replaceAll("(?m)^--[^\\n]*", "").trim();
            for(String name:wanted) if(sql.startsWith("CREATE TABLE "+name+" (")) result.append(sql).append(";\n");
        }
        if(result.toString().split("CREATE TABLE ",-1).length-1!=wanted.size())throw new IllegalArgumentException("Thiếu bảng baseline");
        return result.toString();
    }
    public static String h2(String sql) {
        return sql.replace("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4","")
            .replaceAll("CHARACTER SET (utf8mb4|ascii) COLLATE (utf8mb4_bin|ascii_bin)","")
            .replaceAll("\\bSTORED\\b","")
            .replaceAll("DATE_SUB\\(CURRENT_TIMESTAMP\\(6\\), INTERVAL ([0-9]+) DAY\\)","DATEADD('DAY', -$1, CURRENT_TIMESTAMP(6))")
            .replace("DROP TEMPORARY TABLE ","DROP TABLE ")
            .replace("${devSeedPasswordHash}","!dev-test-login-disabled!");
    }
    public static void execute(Connection c,String sql) throws Exception {
        ScriptUtils.executeSqlScript(c,new ByteArrayResource(h2(sql).getBytes(StandardCharsets.UTF_8)));
    }
    public static void schema(Connection c) throws Exception {execute(c,Files.readString(SCHEMA));}
    public static void seed(Connection c) throws Exception {execute(c,Files.readString(SEED));}
}
