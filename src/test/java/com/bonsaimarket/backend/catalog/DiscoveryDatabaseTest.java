package com.bonsaimarket.backend.catalog;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Chỉ thực thi SQL đã điều chỉnh cú pháp H2; đối chiếu MySQL là test bật riêng. */
class DiscoveryDatabaseTest {
    final StorefrontMigrationSeedTest scripts=new StorefrontMigrationSeedTest();
    void upgrade(Connection c)throws Exception {scripts.schema(c,true);}
    void executeSeed(Connection c)throws Exception {com.bonsaimarket.backend.support.BaselineSql.seed(c);}
    @Test void upgradePreservesExistingRowsAndDevelopmentSeedReplaysWithoutOverwrites()throws Exception {
        try(var c=DriverManager.getConnection("jdbc:h2:mem:discussionupgrade;MODE=MySQL","sa","")){
            upgrade(c);assertEquals(69,scripts.scalar(c,"select count(*) from products"));assertEquals(75,scripts.scalar(c,"select count(*) from orders"));
            assertEquals(4,scripts.scalar(c,"select count(*) from community_posts"));assertEquals(2,scripts.scalar(c,"select count(*) from community_comments"));assertEquals(4,scripts.scalar(c,"select count(*) from product_questions"));
            assertEquals(0,scripts.scalar(c,"select count(*) from product_questions q join products p on p.id=q.product_id join stores s on s.id=p.store_id where q.answer_author_id is not null and q.answer_author_id<>s.owner_user_id"));
            try(var s=c.createStatement()){s.execute("update community_posts set body='Member edit' where seed_key='discussion-v1-care'");s.execute("update product_questions set body='Member question' where seed_key='discussion-v1-question-bonsai-open'");}
            executeSeed(c);assertEquals(4,scripts.scalar(c,"select count(*) from community_posts"));assertEquals(11,scripts.scalar(c,"select count(*) from users"));assertEquals("Member edit",scripts.text(c,"select body from community_posts where seed_key='discussion-v1-care'"));assertEquals("Member question",scripts.text(c,"select body from product_questions where seed_key='discussion-v1-question-bonsai-open'"));
        }
    }
    @Test void bootstrapFinalSchemaAndSeedExecuteInIsolation()throws Exception {
        try(var a=DriverManager.getConnection("jdbc:h2:mem:discussionmigrations;MODE=MySQL","sa","");var b=DriverManager.getConnection("jdbc:h2:mem:discussionbootstrap;MODE=MySQL","sa","")){
            upgrade(a);scripts.schema(b,true);
            assertEquals(columns(a),columns(b),"H2 column metadata parity, not MySQL parity");
            for(String table:List.of("users","categories","stores","products","orders","order_items","reviews","wishlist_items","community_posts","community_comments","product_questions"))assertEquals(scripts.scalar(a,"select count(*) from "+table),scripts.scalar(b,"select count(*) from "+table),table);
            executeSeed(b);
            assertEquals(75,scripts.scalar(b,"select count(*) from orders"));assertEquals(11,scripts.scalar(b,"select count(*) from users"));assertEquals(4,scripts.scalar(b,"select count(*) from community_posts"));
            assertEquals(0,scripts.scalar(b,"select count(*) from users where password_hash not like '!%disabled%'"));
        }
    }
    Set<String> columns(Connection c)throws Exception {
        // H2 hiển thị bảng tạm của seed theo kết nối, MySQL không hiển thị; chỉ so sánh bảng nghiệp vụ.
        var result=new TreeSet<String>();try(var s=c.createStatement();var r=s.executeQuery("select table_name,column_name,ordinal_position,data_type,is_nullable,column_default,generation_expression from information_schema.columns where table_schema='PUBLIC' and table_name not in ('DISCUSSION_SEED_AUTHORS','STOREFRONT_SEED_OWNERS') order by table_name,ordinal_position")){while(r.next()){var row=new ArrayList<String>();for(int i=1;i<=7;i++)row.add(Objects.toString(r.getObject(i),"NULL"));result.add(String.join("|",row));}}return result;
    }
    @Test void consolidatedV1CreatesAllDefinitionsWithH2Adaptations()throws Exception {
        try(var c=DriverManager.getConnection("jdbc:h2:mem:consolidated-definitions;MODE=MySQL","sa","")){
            scripts.schema(c,false);
            assertEquals(48,scripts.scalar(c,"select count(*) from information_schema.tables where table_schema='PUBLIC' and table_type='BASE TABLE'"));
            assertEquals(453,scripts.scalar(c,"select count(*) from information_schema.columns where table_schema='PUBLIC'"));
            for(var entry:Map.of("PRIMARY KEY",48,"FOREIGN KEY",81,"UNIQUE",46,"CHECK",70).entrySet())
                assertEquals(entry.getValue().longValue(),scripts.scalar(c,"select count(*) from information_schema.table_constraints where constraint_schema='PUBLIC' and constraint_type='"+entry.getKey()+"'"));
            assertEquals(1,scripts.scalar(c,"select count(*) from information_schema.columns where table_schema='PUBLIC' and is_generated='ALWAYS'"));
        }
    }
    @Test void onlyCanonicalSchemaAndRepeatableSeedAreActive()throws Exception {
        String config=Files.readString(Path.of("src/main/resources/application.yaml"));assertFalse(config.contains("db/reference"));
        String sql=Files.readString(com.bonsaimarket.backend.support.BaselineSql.SCHEMA);assertFalse(sql.matches("(?is).*\\bDROP\\s+(TABLE|DATABASE)\\b.*"));assertFalse(sql.contains("FOREIGN_KEY_CHECKS"));
        assertFalse(sql.matches("(?is).*CREATE\\s+TABLE\\s+IF\\s+NOT\\s+EXISTS.*"));
        var alters=java.util.regex.Pattern.compile("(?is)ALTER\\s+TABLE\\s+[^;]+;").matcher(sql).results().map(java.util.regex.MatchResult::group).toList();
        assertEquals(List.of("ALTER TABLE community_posts ADD CONSTRAINT fk_community_accepted_comment FOREIGN KEY (accepted_comment_id) REFERENCES community_comments(id);"),alters);
        assertEquals(48,java.util.regex.Pattern.compile("(?m)^CREATE TABLE ").matcher(sql).results().count());
        try(var paths=Files.list(Path.of("src/main/resources/db/migration"))){assertEquals(List.of("V1__initial_schema.sql"),paths.map(p->p.getFileName().toString()).sorted().toList());}
        // Check the actual compiled classpath too: Maven resource copying does not delete stale SQL.
        var resources=new org.springframework.core.io.support.PathMatchingResourcePatternResolver().getResources("classpath*:db/migration/*.sql");
        assertEquals(List.of("V1__initial_schema.sql"),Arrays.stream(resources).map(org.springframework.core.io.Resource::getFilename).sorted().toList());
        assertEquals(sql,new String(resources[0].getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
        try(var paths=Files.list(Path.of("src/main/resources/db/dev"))){assertEquals(List.of("R__dev_seed.sql"),paths.map(p->p.getFileName().toString()).filter(n->n.endsWith(".sql")).sorted().toList());}
    }
}
