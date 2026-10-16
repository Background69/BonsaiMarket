package com.bonsaimarket.backend.auth;

import org.junit.jupiter.api.Test;
import java.sql.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** H2 SQL portability test for additive preservation/constraints. Not proof of MySQL/Flyway execution. */
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
            var directory=Path.of("src/main/resources/db/migration");
            var files=Files.list(directory).filter(path->path.getFileName().toString().matches("V[1-8]__.*\\.sql")).sorted().toList();
            assertEquals(8,files.size());for(var file:files) execute(connection,sql(file));
            execute(connection,sql(Path.of("src/main/resources/db/dev/V9__seed_development_data.sql")));
            Map<String,Integer> before=new HashMap<>();for(String table:List.of("users","stores","products","categories","addresses","carts","cart_items","orders","order_items","reviews","wishlist_items")) before.put(table,count(connection,table));
            // H2 requires one ALTER action per statement; MySQL accepts the shipped multi-action ALTER.
            String migration=sql(directory.resolve("V10__authentication.sql"));int end=migration.indexOf(';');
            String alter=migration.substring(0,end);String actions=alter.substring(alter.indexOf("ADD COLUMN"));
            for(String action:actions.split(",\\s+(?=ADD )")) execute(connection,"ALTER TABLE users "+action);
            execute(connection,migration.substring(end+1));
            for(var entry:before.entrySet()) assertEquals(entry.getValue(),count(connection,entry.getKey()));
            try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT role,email_verified_at,auth_version FROM users WHERE id=1")) {
                assertTrue(rows.next());assertEquals("ADMIN",rows.getString(1));assertNull(rows.getObject(2));assertEquals(0,rows.getLong(3));
            }
            execute(connection,"INSERT INTO oauth_identities(user_id,provider,provider_subject) VALUES(3,'google','subject-one')");
            assertThrows(SQLException.class,()->execute(connection,"INSERT INTO oauth_identities(user_id,provider,provider_subject) VALUES(2,'google','subject-one')"));
            assertThrows(SQLException.class,()->execute(connection,"INSERT INTO oauth_identities(user_id,provider,provider_subject) VALUES(99999,'google','subject-new')"));
            assertThrows(SQLException.class,()->execute(connection,"INSERT INTO users(name,email,password_hash,role) VALUES('Duplicate',' USER@BONSAIMARKET.LOCAL ','hash','CUSTOMER')"));
            assertThrows(SQLException.class,()->execute(connection,"INSERT INTO users(name,email,password_hash,role) VALUES('Invalid','invalid@example.com','hash','BUYER')"));
        }
    }
    private int count(Connection connection,String table) throws SQLException {
        try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT COUNT(*) FROM "+table)) {rows.next();return rows.getInt(1);}
    }
}
