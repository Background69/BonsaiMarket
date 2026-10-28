package com.bonsaimarket.backend.support;

import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

/** Từ chối schema ngoài loopback hoặc có bảng trước khi migrate. */
public final class OwnedMySql {
    private OwnedMySql() {}
    public static void empty(String url,String password)throws Exception {
        assertNotNull(url);
        assertTrue(url.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/bm_(checkout|parity|shipping|live)_[a-z0-9_]+\\?.*"));
        try(var c=DriverManager.getConnection(url,"root",password);var s=c.createStatement()) {
            assertTrue(c.getMetaData().getDatabaseProductVersion().startsWith("8.4."));
            try(var r=s.executeQuery("select count(*) from information_schema.tables where table_schema=database()")) {
                assertTrue(r.next());assertEquals(0,r.getInt(1),"Database riêng phải trống trước khi ghi");
            }
        }
    }
}
