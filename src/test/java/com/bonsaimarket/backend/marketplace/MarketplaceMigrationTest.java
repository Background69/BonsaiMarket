package com.bonsaimarket.backend.marketplace;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Chỉ kiểm tra cú pháp H2 và bảo toàn dữ liệu; cần xác minh Flyway/MySQL riêng. */
class MarketplaceMigrationTest {
    void execute(Connection c,String source) throws Exception {
        String sql=source.replaceAll("(?m)^--[^\\n]*", "").replace("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", "")
            .replaceAll("DATE_SUB\\(CURRENT_TIMESTAMP\\(6\\), INTERVAL ([0-9]+) DAY\\)","DATEADD('DAY', -$1, CURRENT_TIMESTAMP(6))");
        try(var s=c.createStatement()) {for(String command:sql.split(";"))if(!command.isBlank())s.execute(command);}
    }
    @Test void additiveV11PreservesLegacyRowsStocksAndConstraints() throws Exception {
        try(var c=DriverManager.getConnection("jdbc:h2:mem:inventorymigration;MODE=MySQL","sa","")) {
            com.bonsaimarket.backend.support.BaselineSql.schema(c);com.bonsaimarket.backend.support.BaselineSql.seed(c);
            var stocks=new TreeMap<Long,Integer>();try(var s=c.createStatement();var r=s.executeQuery("select id,stock from products")){while(r.next())stocks.put(r.getLong(1),r.getInt(2));}
            var counts=new HashMap<String,Integer>();for(String table:List.of("users","products","orders","addresses","order_items"))counts.put(table,count(c,table));
            com.bonsaimarket.backend.support.BaselineSql.seed(c);
            for(var entry:counts.entrySet())assertEquals(entry.getValue(),count(c,entry.getKey()));
            try(var s=c.createStatement();var r=s.executeQuery("select id,stock from products")){while(r.next())assertEquals(stocks.get(r.getLong(1)),r.getInt(2));}
            int debitsBefore=count(c,"inventory_debits");
            execute(c,"INSERT INTO inventory_debits(order_id,product_id,quantity) VALUES(1,1,1)");
            assertThrows(SQLException.class,()->execute(c,"INSERT INTO inventory_debits(order_id,product_id,quantity) VALUES(1,1,1)"));
            assertThrows(SQLException.class,()->execute(c,"INSERT INTO inventory_debits(order_id,product_id,quantity) VALUES(99999,1,1)"));
            assertThrows(SQLException.class,()->execute(c,"INSERT INTO inventory_debits(order_id,product_id,quantity) VALUES(2,1,0)"));
            assertThrows(SQLException.class,()->execute(c,"UPDATE products SET stock=-1 WHERE id=1"));
            assertEquals(debitsBefore+1,count(c,"inventory_debits"));
        }
    }
    int count(Connection c,String table)throws Exception {try(var s=c.createStatement();var r=s.executeQuery("select count(*) from "+table)){r.next();return r.getInt(1);}}
}
