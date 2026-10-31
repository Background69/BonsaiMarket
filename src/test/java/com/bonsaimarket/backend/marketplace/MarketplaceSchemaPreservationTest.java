package com.bonsaimarket.backend.marketplace;

import com.bonsaimarket.backend.seller.application.*;
import com.bonsaimarket.backend.media.*;
import com.bonsaimarket.backend.report.*;
import com.bonsaimarket.backend.profile.*;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Kiểm tra ràng buộc và bảo toàn bằng H2; MySQL UPDATE JOIN/Flyway vẫn NOT VERIFIED. */
class MarketplaceSchemaPreservationTest {
    void execute(Connection c,String source)throws Exception{
        String sql=source.replace("\r\n","\n").replaceAll("(?m)^--[^\\n]*","").replace("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4","")
            .replaceAll("DATE_SUB\\(CURRENT_TIMESTAMP\\(6\\), INTERVAL ([0-9]+) DAY\\)","DATEADD('DAY', -$1, CURRENT_TIMESTAMP(6))")
            .replace("UPDATE categories c JOIN (SELECT MIN(id) AS id FROM categories GROUP BY LOWER(TRIM(name))) canonical ON canonical.id=c.id\nSET c.name_key=LOWER(TRIM(c.name))",
                "UPDATE categories SET name_key=LOWER(TRIM(name)) WHERE id IN (SELECT MIN(id) FROM categories GROUP BY LOWER(TRIM(name)))");
        try(var s=c.createStatement()){for(var statement:sql.split(";"))if(!statement.isBlank())s.execute(statement);}
    }
    int count(Connection c,String table)throws Exception{try(var s=c.createStatement();var r=s.executeQuery("select count(*) from "+table)){r.next();return r.getInt(1);}}
    @Test void v12PreservesLegacyDataAndAddsConstraintsWithoutApprovingUsers()throws Exception{
        try(var c=DriverManager.getConnection("jdbc:h2:mem:managementmigration;MODE=MySQL","sa","")){
            com.bonsaimarket.backend.support.BaselineSql.schema(c);com.bonsaimarket.backend.support.BaselineSql.seed(c);
            // Tên trùng trong dữ liệu cũ không được làm migration xóa hoặc ghi đè danh mục.
            execute(c,"INSERT INTO categories(name,slug) VALUES('Bonsai','historical-duplicate')");
            var counts=new LinkedHashMap<String,Integer>();for(var table:List.of("users","stores","categories","products","addresses","orders","order_items","reviews"))counts.put(table,count(c,table));
            var roles=new TreeMap<Long,String>();try(var s=c.createStatement();var rows=s.executeQuery("select id,role from users")){while(rows.next())roles.put(rows.getLong(1),rows.getString(2));}
            com.bonsaimarket.backend.support.BaselineSql.seed(c);
            for(var entry:counts.entrySet())assertEquals(entry.getValue(),count(c,entry.getKey()));
            try(var s=c.createStatement();var rows=s.executeQuery("select id,role from users")){while(rows.next())assertEquals(roles.get(rows.getLong(1)),rows.getString(2));}
            assertEquals(0,count(c,"seller_applications"));assertEquals(0,count(c,"product_images"));
            execute(c,"INSERT INTO seller_applications(applicant_id,status,name,description,phone,address,submitted_at) VALUES(2,'PENDING','New','','','',CURRENT_TIMESTAMP)");
            assertThrows(SQLException.class,()->execute(c,"INSERT INTO seller_applications(applicant_id,status,name,description,phone,address,submitted_at) VALUES(2,'PENDING','Other','','','',CURRENT_TIMESTAMP)"));
            assertThrows(SQLException.class,()->execute(c,"UPDATE seller_applications SET status='ACTIVE'"));
            execute(c,"INSERT INTO marketplace_reports(reporter_id,target_type,target_id,reason,description,status,created_at) VALUES(2,'PRODUCT',1,'FRAUD','','OPEN',CURRENT_TIMESTAMP)");
            assertThrows(SQLException.class,()->execute(c,"INSERT INTO marketplace_reports(reporter_id,target_type,target_id,reason,description,status,created_at) VALUES(2,'PRODUCT',1,'FRAUD','','OPEN',CURRENT_TIMESTAMP)"));
            assertThrows(SQLException.class,()->execute(c,"UPDATE marketplace_reports SET target_type='USER'"));
            assertThrows(SQLException.class,()->execute(c,"INSERT INTO product_images(product_id,file_key,content_type,bytes) VALUES(999999,'missing.png','image/png',1)"));
            assertThrows(SQLException.class,()->execute(c,"DELETE FROM categories WHERE id=1"));
            execute(c,"INSERT INTO categories(name,slug,name_key) VALUES('New','new','new')");
            assertThrows(SQLException.class,()->execute(c,"INSERT INTO categories(name,slug,name_key) VALUES('NEW','new-again','new')"));
            try(var s=c.createStatement();var r=s.executeQuery("select count(*) from stores where revision<>0")){r.next();assertEquals(0,r.getInt(1));}
        }
    }
}
