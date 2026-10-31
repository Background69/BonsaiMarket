package com.bonsaimarket.backend.auth;

import org.junit.jupiter.api.Test;
import java.sql.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Kiểm tra SQL, ràng buộc và bảo toàn dữ liệu qua H2; không chứng minh MySQL/Flyway thực thi. */
class AuthMigrationTest {
    private String sql(Path path) throws Exception {
        return Files.readString(path).replaceAll("(?m)^--[^\\n]*", "")
            .replaceAll("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", "")
            .replaceAll("CHARACTER SET (utf8mb4|ascii) COLLATE (utf8mb4_bin|ascii_bin)", "")
            .replaceAll("DATE_SUB\\(CURRENT_TIMESTAMP\\(6\\), INTERVAL ([0-9]+) DAY\\)", "DATEADD('DAY', -$1, CURRENT_TIMESTAMP(6))")
            .replaceAll("\\bSTORED\\b", "");
    }
    private void execute(Connection connection,String script) throws SQLException {
        try(var statement=connection.createStatement()) {for(String command:script.split(";")) if(!command.isBlank()) statement.execute(command);}
    }
    @Test void additiveMigrationPreservesLegacySeedAndAddsAuthConstraints() throws Exception {
        try(var connection=DriverManager.getConnection("jdbc:h2:mem:auth-migration;MODE=MySQL", "sa", "")) {
            com.bonsaimarket.backend.support.BaselineSql.schema(connection);com.bonsaimarket.backend.support.BaselineSql.seed(connection);
            Map<String,Integer> before=new HashMap<>();for(String table:List.of("users","stores","products","categories","addresses","carts","cart_items","orders","order_items","reviews","wishlist_items")) before.put(table,count(connection,table));
            com.bonsaimarket.backend.support.BaselineSql.seed(connection);
            for(var entry:before.entrySet()) assertEquals(entry.getValue(),count(connection,entry.getKey()));
            try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT role,email_verified_at,auth_version FROM users WHERE id=1")) {
                assertTrue(rows.next());assertEquals("ADMIN",rows.getString(1));assertNotNull(rows.getObject(2));assertEquals(0,rows.getLong(3));
            }
            execute(connection,"INSERT INTO oauth_identities(user_id,provider,provider_subject) VALUES(3,'google','subject-one')");
            assertThrows(SQLException.class,()->execute(connection,"INSERT INTO oauth_identities(user_id,provider,provider_subject) VALUES(2,'google','subject-one')"));
            assertThrows(SQLException.class,()->execute(connection,"INSERT INTO oauth_identities(user_id,provider,provider_subject) VALUES(99999,'google','subject-new')"));
            assertThrows(SQLException.class,()->execute(connection,"INSERT INTO users(name,email,password_hash,role) VALUES('Duplicate',' USER@BONSAI.MARKET ','hash','CUSTOMER')"));
            assertThrows(SQLException.class,()->execute(connection,"INSERT INTO users(name,email,password_hash,role) VALUES('Invalid','invalid@example.com','hash','BUYER')"));
        }
    }
    private int count(Connection connection,String table) throws SQLException {
        try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT COUNT(*) FROM "+table)) {rows.next();return rows.getInt(1);}
    }
}
