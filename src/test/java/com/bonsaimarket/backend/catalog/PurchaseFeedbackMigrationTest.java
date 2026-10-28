package com.bonsaimarket.backend.catalog;

import org.junit.jupiter.api.Test;
import java.sql.*;
import java.nio.file.*;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.core.io.ByteArrayResource;
import static org.junit.jupiter.api.Assertions.*;

/** Chạy V18 trên SQL lịch sử có seed, chỉ điều chỉnh một cú pháp DDL cho H2. */
class PurchaseFeedbackMigrationTest {
    @Test void additiveUpgradePreservesLegacyReviewsAndOfficialAnswersAndEnforcesNewConstraints() throws Exception {
        try(var c=DriverManager.getConnection("jdbc:h2:mem:feedback-migration;MODE=MySQL","sa","")) {
            var previous=new StorefrontMigrationSeedTest();previous.schema(c,true);
            try(var s=c.createStatement()) {s.execute("insert into product_questions(product_id,author_id,body,answer_author_id,answer_body,answered_at) values(1,3,'Question',2,'Official',CURRENT_TIMESTAMP)");}
            long count=previous.scalar(c,"select count(*) from reviews");
            assertEquals(count,previous.scalar(c,"select count(*) from reviews"));
            assertEquals(count,previous.scalar(c,"select count(*) from reviews where order_item_id is null"));
            assertEquals("Official",previous.text(c,"select answer_body from product_questions where body='Question'"));
            try(var s=c.createStatement()) {
                s.execute("insert into reviews(product_id,user_id,order_item_id,rating,comment) values(1,3,1,5,'Purchase one')");
                assertThrows(SQLException.class,()->s.execute("insert into reviews(product_id,user_id,order_item_id,rating,comment) values(1,3,1,4,'Duplicate')"));
                s.execute("insert into reviews(product_id,user_id,order_item_id,rating,comment) values(1,3,2,4,'Purchase two')");
                assertThrows(SQLException.class,()->s.execute("insert into reviews(product_id,user_id,order_item_id,rating) values(1,3,999999,5)"));
                assertThrows(SQLException.class,()->s.execute("insert into reviews(product_id,user_id,rating) values(1,3,6)"));
                s.execute("insert into product_question_replies(question_id,author_id,body) select id,3,'Member reply' from product_questions where body='Question'");
                assertThrows(SQLException.class,()->s.execute("insert into product_question_replies(question_id,author_id,body) values(999999,3,'Wrong parent')"));
                assertThrows(SQLException.class,()->s.execute("insert into product_question_replies(question_id,author_id,body) values(1,3,'')"));
            }
        }
    }
}
