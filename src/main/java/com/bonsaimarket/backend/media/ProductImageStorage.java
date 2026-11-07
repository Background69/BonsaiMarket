package com.bonsaimarket.backend.media;

import com.bonsaimarket.backend.marketplace.MarketplaceException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.time.Clock;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** The existing product image root; only server-generated PNG keys are accepted. */
@Component
public class ProductImageStorage {
    public record Prepared(byte[] bytes,byte[] thumbnail,int width,int height,String checksum){}
    public record Stored(String key,String thumbnailKey){}
    private final Path root;private final long maxBytes;private final Clock clock;
    private final java.util.concurrent.Semaphore processing=new java.util.concurrent.Semaphore(2);
    public ProductImageStorage(@Value("${marketplace.media.root:.local/product-images}") String root,
        @Value("${marketplace.media.max-bytes:5242880}") long maxBytes,Clock clock){
        this.root=Path.of(root).toAbsolutePath().normalize();this.maxBytes=maxBytes;this.clock=clock;
    }
    public long maxBytes(){return maxBytes;}
    public Prepared prepare(MultipartFile file){
        if(!processing.tryAcquire())throw new MarketplaceException(429,"IMAGE_PROCESSING_BUSY","Máy chủ đang xử lý ảnh khác. Giữ mục này trong hàng đợi và thử lại sau.");
        try{
            var decoded=SafeImage.decodeProduct(file,maxBytes);
            var image=new BufferedImage(decoded.getWidth(),decoded.getHeight(),BufferedImage.TYPE_INT_ARGB);
            var canvas=image.createGraphics();try{canvas.drawImage(decoded,0,0,null);}finally{canvas.dispose();}
            byte[] bytes=encode(image);
            double scale=Math.min(1,240d/Math.max(image.getWidth(),image.getHeight()));
            var thumbnail=new BufferedImage(Math.max(1,(int)Math.round(image.getWidth()*scale)),Math.max(1,(int)Math.round(image.getHeight()*scale)),BufferedImage.TYPE_INT_ARGB);
            var graphics=thumbnail.createGraphics();try{graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);graphics.drawImage(image,0,0,thumbnail.getWidth(),thumbnail.getHeight(),null);}finally{graphics.dispose();}
            return new Prepared(bytes,encode(thumbnail),image.getWidth(),image.getHeight(),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        }catch(IOException e){throw new MarketplaceException(503,"IMAGE_PROCESSING_UNAVAILABLE","Chưa thể xử lý ảnh. Bản gốc của bạn không bị thay đổi.");}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 unavailable",e);}
        finally{processing.release();}
    }
    private byte[] encode(BufferedImage image)throws IOException{
        var output=new ByteArrayOutputStream();final boolean[] exceeded={false};
        try(var bounded=new FilterOutputStream(output){
            private long written;
            private void check(int length)throws IOException{if(written+length>maxBytes){exceeded[0]=true;throw new IOException("Encoded image exceeds configured limit");}written+=length;}
            @Override public void write(int value)throws IOException{check(1);out.write(value);}
            @Override public void write(byte[] bytes,int offset,int length)throws IOException{check(length);out.write(bytes,offset,length);}
        }){if(!ImageIO.write(image,"png",bounded))throw new IOException("PNG writer unavailable");}
        catch(IOException e){if(exceeded[0])throw new MarketplaceException(413,"IMAGE_TOO_LARGE","Ảnh sau xử lý vượt giới hạn dung lượng. Chọn ảnh nhỏ hơn.");throw e;}
        return output.toByteArray();
    }
    public Stored store(Prepared image){
        String id=UUID.randomUUID().toString(),key=id+".png",thumb=id+"-thumb.png";
        // Journal must exist durably BEFORE any image is created. On rollback/crash it remains retryable.
        deferCleanup(java.util.List.of(key,thumb));
        try{Files.write(path(key),image.bytes(),StandardOpenOption.CREATE_NEW);Files.write(path(thumb),image.thumbnail(),StandardOpenOption.CREATE_NEW);
            for(String written:java.util.List.of(key,thumb))try(var channel=java.nio.channels.FileChannel.open(path(written),StandardOpenOption.WRITE)){channel.force(true);}
            return new Stored(key,thumb);}
        catch(IOException e){throw new MarketplaceException(503,"MEDIA_UNAVAILABLE","Không thể lưu ảnh vào storage. Giữ ảnh trong hàng đợi để thử lại cùng yêu cầu.");}
    }
    public void deferCleanup(java.util.List<String> keys){
        deferCleanup(keys,java.util.List.of());
    }
    public void deferCleanup(java.util.List<String> keys,java.util.List<String> urls){
        if(keys.isEmpty())return;keys.forEach(this::path);
        if(!org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()||!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Storage mutations require a transaction");
        try{
            Files.createDirectories(root);Files.createDirectories(journalRoot());
            String id=UUID.randomUUID().toString();Path temporary=journalRoot().resolve(id+".tmp"),ready=journalRoot().resolve(id+".properties");
            var properties=new Properties();properties.setProperty("created",Long.toString(clock.millis()));properties.setProperty("keys",String.join(",",keys));
            properties.setProperty("urls",String.join("\n",urls));
            try(var output=Files.newOutputStream(temporary,StandardOpenOption.CREATE_NEW)){properties.store(output,"Product gallery cleanup intent");}
            // Force the intent before publishing it; files are never created if publishing fails.
            try(var channel=java.nio.channels.FileChannel.open(temporary,StandardOpenOption.WRITE)){channel.force(true);}
            Files.move(temporary,ready,StandardCopyOption.ATOMIC_MOVE);
            var channel=java.nio.channels.FileChannel.open(ready,StandardOpenOption.READ,StandardOpenOption.WRITE);
            java.nio.channels.FileLock lock;
            try{lock=channel.lock();}catch(IOException|RuntimeException e){channel.close();throw e;}
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){
                @Override public void afterCompletion(int status){try(channel){lock.release();}catch(IOException e){org.slf4j.LoggerFactory.getLogger(ProductImageStorage.class).warn("Gallery cleanup intent retained after lock release failure.");}}
            });
        }catch(IOException e){throw new MarketplaceException(503,"MEDIA_JOURNAL_UNAVAILABLE","Chưa thể ghi nhận tác vụ storage an toàn. Thao tác chưa được áp dụng.");}
    }
    Path journalRoot(){return root.resolve(".gallery-cleanup");}
    Path path(String key){
        if(key==null||!key.matches("[0-9a-f-]{36}(-thumb)?\\.png"))throw MarketplaceException.missing();
        Path result=root.resolve(key).normalize();if(!root.equals(result.getParent()))throw MarketplaceException.missing();return result;
    }
    public byte[] read(String key){try{return Files.readAllBytes(path(key));}catch(IOException e){throw MarketplaceException.missing();}}
}
