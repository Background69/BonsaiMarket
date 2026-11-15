package com.bonsaimarket.backend.management;

import com.bonsaimarket.backend.support.BaselineSql;

import java.sql.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminNotificationMigrationTest {
    @Test
    void newSchemaKeepsEmailTargetGuardAndAddsInAppOwnershipReadState() throws Exception {
        try (var c = DriverManager.getConnection("jdbc:h2:mem:admin-notification-migration;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""); var s = c.createStatement()) {
            BaselineSql.currentSchema(c);
            s.execute("insert into users(id,name,email,password_hash,role,is_active) values(1,'QA','qa@example.invalid','!disabled!','ADMIN',true)");
            String prefix = "insert into order_notification_outbox(id,event_type,recipient,subject,body,dedup_key,status,attempts,next_attempt_at,updated_at,created_at,channel,recipient_user_id,target_link) values(";
            s.execute(prefix + "'inapp','ADMIN_ACTIVITY','qa@example.invalid','Work','Body','key1','PENDING',0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'IN_APP',1,'/admin/reports')");
            assertThrows(SQLException.class, () -> s.execute(prefix + "'missing-owner','ADMIN_ACTIVITY','qa@example.invalid','Work','Body','key2','PENDING',0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'IN_APP',null,'/admin/reports')"));
            assertThrows(SQLException.class, () -> s.execute(prefix + "'bad-email','ADMIN_ACTIVITY','qa@example.invalid','Work','Body','key3','PENDING',0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'EMAIL',null,null)"));
            s.execute("update order_notification_outbox set read_at=CURRENT_TIMESTAMP where id='inapp'");
            try (var r = s.executeQuery("select status,read_at from order_notification_outbox where id='inapp'")) {
                assertTrue(r.next());
                assertEquals("PENDING", r.getString(1));
                assertNotNull(r.getTimestamp(2));
            }
            assertThrows(SQLException.class, () -> s.execute(prefix + "'orphan','ADMIN_ACTIVITY','qa@example.invalid','Work','Body','key4','PENDING',0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'IN_APP',999,'/admin/reports')"));
        }
    }
}
