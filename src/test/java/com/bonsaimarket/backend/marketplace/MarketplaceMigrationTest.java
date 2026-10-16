package com.bonsaimarket.backend.marketplace;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** H2 syntax adaptation/preservation only. MySQL Flyway execution must be checked separately. */
class MarketplaceMigrationTest {
    void execute(Connection c,String source) throws Exception {
        String sql=source.replaceAll("(?m)^--[^\\n]*", "").replace("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", "")
            .replaceAll("DATE_SUB\\(CURRENT_TIMESTAMP\\(6\\), INTERVAL ([0-9]+) DAY\\)","DATEADD('DAY', -$1, CURRENT_TIMESTAMP(6))");
        try(var s=c.createStatement()) {for(String command:sql.split(";"))if(!command.isBlank())s.execute(command);}
    }
    @Test void additiveV11PreservesLegacyRowsStocksAndConstraints() throws Exception {
        try(var c=DriverManager.getConnection("jdbc:h2:mem:inventorymigration;MODE=MySQL","sa","")) {
            var directory=Path.of("src/main/resources/db/migration");
            try(var paths=Files.list(directory)) {for(var file:paths.filter(p->p.getFileName().toString().matches("V[1-8]__.*")).sorted().toList())execute(c,Files.readString(file));}
            execute(c,Files.readString(Path.of("src/main/resources/db/dev/V9__seed_development_data.sql")));
            var stocks=new TreeMap<Long,Integer>();try(var s=c.createStatement();var r=s.executeQuery("select id,stock from products")){while(r.next())stocks.put(r.getLong(1),r.getInt(2));}
            var counts=new HashMap<String,Integer>();for(String table:List.of("users","products","orders","addresses","order_items"))counts.put(table,count(c,table));
            execute(c,Files.readString(directory.resolve("V11__marketplace_inventory_and_catalog_indexes.sql")));
            for(var entry:counts.entrySet())assertEquals(entry.getValue(),count(c,entry.getKey()));
            try(var s=c.createStatement();var r=s.executeQuery("select id,stock from products")){while(r.next())assertEquals(stocks.get(r.getLong(1)),r.getInt(2));}
            execute(c,"INSERT INTO inventory_debits(order_id,product_id,quantity) VALUES(1,1,1)");
            assertThrows(SQLException.class,()->execute(c,"INSERT INTO inventory_debits(order_id,product_id,quantity) VALUES(1,1,1)"));
            assertThrows(SQLException.class,()->execute(c,"INSERT INTO inventory_debits(order_id,product_id,quantity) VALUES(99999,1,1)"));
            assertThrows(SQLException.class,()->execute(c,"INSERT INTO inventory_debits(order_id,product_id,quantity) VALUES(2,1,0)"));
            assertThrows(SQLException.class,()->execute(c,"UPDATE products SET stock=-1 WHERE id=1"));
            assertEquals(1,count(c,"inventory_debits"));
        }
    }
    int count(Connection c,String table)throws Exception {try(var s=c.createStatement();var r=s.executeQuery("select count(*) from "+table)){r.next();return r.getInt(1);}}
}
