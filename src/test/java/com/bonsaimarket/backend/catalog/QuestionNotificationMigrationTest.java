package com.bonsaimarket.backend.catalog;

import com.bonsaimarket.backend.support.BaselineSql;
import java.nio.file.Files;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuestionNotificationMigrationTest {
    @Test void sharedOutboxMigrationPreservesOrderForeignKeyAndRequiresQuestionTargetForNullOrder() throws Exception {
        try(var c=DriverManager.getConnection("jdbc:h2:mem:question-outbox-migration;MODE=MySQL","sa","")) {
            BaselineSql.execute(c,Files.readString(BaselineSql.SCHEMA));
            try(var s=c.createStatement()) {
                String row="insert into order_notification_outbox(id,order_id,event_type,recipient,subject,body,dedup_key,status,attempts,next_attempt_at,updated_at,created_at) values(";
                s.execute(row+"'q1',null,'PRODUCT_QUESTION_ASKED','qa@example.invalid','Question','Public question link','q1','PENDING',0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
                assertThrows(SQLException.class,()->s.execute(row+"'bad',null,'CONFIRMED','qa@example.invalid','Order','Body','bad','PENDING',0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"));
                assertThrows(SQLException.class,()->s.execute(row+"'missing',99999,'CONFIRMED','qa@example.invalid','Order','Body','missing','PENDING',0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"));
            }
        }
    }
}
