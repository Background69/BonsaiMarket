package com.bonsaimarket.backend.community;

import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.media.SafeImage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;

@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class CommunityImages {
    private final CommunityService posts;
    private final org.springframework.jdbc.core.JdbcTemplate db;
    private final ContentAccess guard;
    private final Path root;
    public CommunityImages(CommunityService posts,org.springframework.jdbc.core.JdbcTemplate db,ContentAccess guard,@Value("${marketplace.media.root:.local/product-images}") String root){this.posts=posts;this.db=db;this.guard=guard;this.root=Path.of(root).toAbsolutePath().normalize().resolve("community");}
    public CommunityService.Post upload(Long actor,long postId,long revision,String requestKey,MultipartFile file){
        guard.writer(actor);posts.lockPost(postId);var post=posts.rawPost(postId);posts.author(actor,post.authorId());posts.writable(post);
        String key=CommunityService.requestKey(requestKey);if(key==null)throw MarketplaceException.invalid("Cần mã tải ảnh.");
        if(db.queryForObject("select count(*) from community_images where post_id=? and request_key=?",Long.class,postId,key)>0)return posts.post(postId);
        MarketplaceAccess.revision(post.revision(),revision);
        if(db.queryForObject("select count(*) from community_images where post_id=?",Long.class,postId)>=4)throw MarketplaceException.invalid("Mỗi bài tối đa 4 ảnh.");
        Path target=null;
        try{
            var decoded=SafeImage.decode(file,5242880);String id=UUID.randomUUID().toString(),fileKey=UUID.randomUUID()+".png";target=path(fileKey);Files.createDirectories(root);
            try(var output=Files.newOutputStream(target,StandardOpenOption.CREATE_NEW)){if(!ImageIO.write(decoded,"png",output))throw MarketplaceException.invalid("Chưa thể xử lý ảnh.");}
            long bytes=Files.size(target);if(bytes>5242880)throw new MarketplaceException(413,"IMAGE_TOO_LARGE","Ảnh sau xử lý vượt 5 MB.");
            Path created=target;TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)remove(created);}});
            db.update("insert into community_images(id,post_id,file_key,bytes,request_key,created_at) values(?,?,?,?,?,?)",id,postId,fileKey,bytes,key,guard.access.now());
            db.update("update community_posts set revision=revision+1,updated_at=? where id=?",guard.access.now(),postId);return posts.post(postId);
        }catch(IOException e){if(target!=null)remove(target);throw new MarketplaceException(503,"MEDIA_UNAVAILABLE","Chưa thể lưu ảnh; nội dung bài đã lưu vẫn được giữ.");}
        catch(RuntimeException e){if(target!=null)remove(target);throw e;}
    }
    public CommunityService.Post delete(Long actor,long postId,String id,long revision){
        guard.writer(actor);posts.lockPost(postId);var post=posts.rawPost(postId);posts.author(actor,post.authorId());posts.writable(post);MarketplaceAccess.revision(post.revision(),revision);
        var rows=db.queryForList("select file_key from community_images where id=? and post_id=?",id,postId);if(rows.isEmpty())throw MarketplaceException.missing();Path old=path((String)rows.getFirst().get("file_key"));
        db.update("delete from community_images where id=? and post_id=?",id,postId);db.update("update community_posts set revision=revision+1,updated_at=? where id=?",guard.access.now(),postId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status==STATUS_COMMITTED)remove(old);}});return posts.post(postId);
    }
    @Transactional(readOnly=true) public byte[] read(long postId,String id){
        posts.rawPost(postId);var keys=db.queryForList("select file_key from community_images where id=? and post_id=?",String.class,id,postId);if(keys.isEmpty())throw MarketplaceException.missing();
        try{return Files.readAllBytes(path(keys.getFirst()));}catch(IOException e){throw MarketplaceException.missing();}
    }
    private Path path(String key){if(!key.matches("[0-9a-f-]{36}\\.png"))throw MarketplaceException.missing();var path=root.resolve(key).normalize();if(!path.getParent().equals(root))throw MarketplaceException.missing();return path;}
    private void remove(Path path){try{Files.deleteIfExists(path);}catch(IOException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Community image cleanup deferred; orphan maintenance required.");}}
}
