package com.bonsaimarket.backend.catalog;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** SQL execution with H2 adaptations only; MySQL parity is a separate opt-in test. */
class DiscoveryDatabaseTest {
    final StorefrontMigrationSeedTest scripts=new StorefrontMigrationSeedTest();
    void upgrade(Connection c)throws Exception {
        scripts.schema(c,true);scripts.execute(c,Path.of("src/main/resources/db/dev/V14__seed_storefront_catalog.sql"));
        for(String name:List.of("V15__product_questions_and_answers.sql","V16__community_posts_and_comments.sql"))scripts.execute(c,Path.of("src/main/resources/db/migration",name));
        executeSeed(c);
        // Compare the current reference with the same complete schema, retaining legacy seed rows.
        try(var paths=Files.list(Path.of("src/main/resources/db/migration"))){
            var latest=paths.filter(p->p.getFileName().toString().matches("V[0-9]+__.*\\.sql"))
                .filter(p->version(p)>=18).sorted(Comparator.comparingInt(DiscoveryDatabaseTest::version)).toList();
            for(var migration:latest)scripts.execute(c,migration);
        }
    }
    static int version(Path path){return Integer.parseInt(path.getFileName().toString().split("__")[0].substring(1));}
    void executeSeed(Connection c)throws Exception {
        String sql=Files.readString(Path.of("src/main/resources/db/dev/V17__seed_community_and_product_questions.sql")).replace("DROP TEMPORARY TABLE discussion_seed_authors","DROP TABLE discussion_seed_authors");
        org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(c,new org.springframework.core.io.ByteArrayResource(sql.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
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
            upgrade(a);scripts.execute(b,Path.of("src/main/resources/db/reference/bonsaimarket_dev_bootstrap.sql"));
            assertEquals(columns(a),columns(b),"H2 column metadata parity, not MySQL parity");
            for(String table:List.of("users","categories","stores","products","orders","order_items","reviews","wishlist_items","community_posts","community_comments","product_questions"))assertEquals(scripts.scalar(a,"select count(*) from "+table),scripts.scalar(b,"select count(*) from "+table),table);
            String seed=Files.readString(Path.of("src/main/resources/db/reference/bonsaimarket_dev_bootstrap.sql")).split("-- PART C: DEVELOPMENT SEED ONLY")[1];
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(b,new org.springframework.core.io.ByteArrayResource(seed.replaceAll("DATE_SUB\\(CURRENT_TIMESTAMP\\(6\\), INTERVAL ([0-9]+) DAY\\)","DATEADD('DAY', -$1, CURRENT_TIMESTAMP(6))").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            assertEquals(75,scripts.scalar(b,"select count(*) from orders"));assertEquals(11,scripts.scalar(b,"select count(*) from users"));assertEquals(4,scripts.scalar(b,"select count(*) from community_posts"));
            assertEquals(0,scripts.scalar(b,"select count(*) from users where password_hash not like '!%disabled%'"));
        }
    }
    Set<String> columns(Connection c)throws Exception {
        // H2 exposes connection-local seed work tables here; MySQL does not. Compare business tables.
        var result=new TreeSet<String>();try(var s=c.createStatement();var r=s.executeQuery("select table_name,column_name,ordinal_position,data_type,is_nullable,column_default,generation_expression from information_schema.columns where table_schema='PUBLIC' and table_name not in ('DISCUSSION_SEED_AUTHORS','STOREFRONT_SEED_OWNERS') order by table_name,ordinal_position")){while(r.next()){var row=new ArrayList<String>();for(int i=1;i<=7;i++)row.add(Objects.toString(r.getObject(i),"NULL"));result.add(String.join("|",row));}}return result;
    }
    @Test void referenceIsIsolatedAndOriginalMigrationFilesAreStillPresent()throws Exception {
        String config=Files.readString(Path.of("src/main/resources/application.yaml"));assertFalse(config.contains("db/reference"));
        String sql=Files.readString(Path.of("src/main/resources/db/reference/bonsaimarket_dev_bootstrap.sql"));assertFalse(sql.matches("(?is).*\\bDROP\\s+(TABLE|DATABASE)\\b.*"));assertFalse(sql.contains("FOREIGN_KEY_CHECKS"));assertFalse(sql.contains("ALTER TABLE"));
        for(int n=1;n<=17;n++){final int version=n;try(var paths=Files.walk(Path.of("src/main/resources/db"),2)){assertEquals(1,paths.filter(p->!p.toString().contains("reference")&&p.getFileName().toString().startsWith("V"+version+"__")).count());}}
    }
}
