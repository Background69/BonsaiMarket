package com.bonsaimarket.backend.community;

import com.bonsaimarket.backend.support.BaselineSql;
import java.nio.file.*;
import java.sql.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommunityMigrationTest {
    @Test void consolidatedSchemaSeedReplayPreservesRowsAndEnforcesCommunityConstraints()throws Exception {
        try(var c=DriverManager.getConnection("jdbc:h2:mem:community-upgrade;MODE=MySQL","sa","");var s=c.createStatement()){
            BaselineSql.schema(c);BaselineSql.seed(c);
            s.executeUpdate("update community_posts set body='Author edit preserved',revision=4 where seed_key='discussion-v1-care'");
            s.executeUpdate("update users set password_hash='owner-edited' where email='seller@bonsai.market'");
            assertEquals(48,scalar(c,"select count(*) from information_schema.tables where table_schema='PUBLIC' and table_type='BASE TABLE'"));
            assertEquals(1,scalar(c,"select count(*) from community_posts where body='Author edit preserved' and revision=4 and post_type='GUIDE'"));
            BaselineSql.seed(c);
            assertEquals(1,scalar(c,"select count(*) from users where email='seller@bonsai.market' and password_hash='owner-edited'"));
            assertEquals(4,scalar(c,"select count(*) from community_posts"));assertEquals(2,scalar(c,"select count(*) from community_comments"));
            long user=scalar(c,"select min(id) from users"),post=scalar(c,"select min(id) from community_posts");
            String like="insert into community_likes(user_id,post_id,created_at) values("+user+","+post+",CURRENT_TIMESTAMP)";s.execute(like);assertThrows(SQLException.class,()->s.execute(like));
            assertThrows(SQLException.class,()->s.execute("insert into community_likes(user_id,post_id,created_at) values("+user+",999999,CURRENT_TIMESTAMP)"));
            assertThrows(SQLException.class,()->s.execute("update community_posts set post_type='FAKE' where id="+post));
            assertThrows(SQLException.class,()->s.execute("update community_posts set help_status='CURED_AUTOMATICALLY' where id="+post));
            assertThrows(SQLException.class,()->s.execute("update community_posts set accepted_comment_id=999999 where id="+post));
            assertEquals(0,scalar(c,"select count(*) from community_bookmarks"));assertEquals(0,scalar(c,"select count(*) from community_images"));
        }
    }
    private long scalar(Connection c,String sql)throws Exception{try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
}
