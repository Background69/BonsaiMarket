package com.bonsaimarket.backend.media;

import jakarta.persistence.EntityManager;
import java.io.*;
import java.nio.file.*;
import java.time.Clock;
import java.util.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Bounded retry of explicit intents only; never scans/deletes arbitrary existing product files. */
@Component
public class ProductImageCleanup {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(ProductImageCleanup.class);
    private final ProductImageStorage storage;private final EntityManager em;private final Clock clock;private final TransactionTemplate tx;
    public ProductImageCleanup(ProductImageStorage storage,EntityManager em,Clock clock,PlatformTransactionManager manager){this.storage=storage;this.em=em;this.clock=clock;tx=new TransactionTemplate(manager);tx.setReadOnly(true);}
    @Scheduled(fixedDelayString="${marketplace.media.cleanup-delay-ms:600000}",initialDelayString="${marketplace.media.cleanup-delay-ms:600000}")
    public void retry(){
        Path folder=storage.journalRoot();if(!Files.isDirectory(folder)||Files.isSymbolicLink(folder))return;
        try(var files=Files.list(folder)){
            for(var intent:files.filter(p->p.getFileName().toString().matches("[0-9a-f-]{36}\\.properties")).limit(20).toList())process(intent);
        }catch(IOException e){LOG.warn("Gallery cleanup journal is unavailable; retry deferred.");}
    }
    void process(Path intent){
        try(var channel=java.nio.channels.FileChannel.open(intent,StandardOpenOption.READ,StandardOpenOption.WRITE)){
            java.nio.channels.FileLock activeLock;
            try{activeLock=channel.tryLock();}catch(java.nio.channels.OverlappingFileLockException e){return;}
            if(activeLock==null)return;
            try(var heldLock=activeLock){
            if(Files.isSymbolicLink(intent)||channel.size()>4096)return;
            var bytes=java.nio.ByteBuffer.allocate((int)channel.size());while(bytes.hasRemaining()&&channel.read(bytes)>=0){}
            var properties=new Properties();try(var input=new ByteArrayInputStream(bytes.array())){properties.load(input);}
            long created=Long.parseLong(properties.getProperty("created","0"));
            // Upload transaction timeout is 60s; the 15 minute grace also protects in-flight requests.
            if(created<=0||clock.millis()-created<900000)return;
            var keys=List.of(properties.getProperty("keys","").split(","));if(keys.isEmpty()||keys.size()>2)return;
            var urls=Arrays.stream(properties.getProperty("urls","").split("\n")).filter(value->!value.isBlank()).toList();
            if(keys.stream().anyMatch(key->!key.matches("[0-9a-f-]{36}(-thumb)?\\.png"))){LOG.warn("Invalid gallery cleanup intent retained for manual review.");return;}
            for(String key:keys){
                Path file=storage.path(key);if(Files.isSymbolicLink(file))return;
                Boolean referenced=tx.execute(status->ProductImageReferences.storage(em,key)||ProductImageReferences.urls(em,urls,null,null));
                if(referenced==null)return;
                if(Boolean.FALSE.equals(referenced))Files.deleteIfExists(file);
            }
            }
            // Close/release the Windows handle before removing the journal itself.
            channel.close();Files.deleteIfExists(intent);
        }catch(IOException|IllegalArgumentException e){LOG.warn("Gallery cleanup intent retained for retry or manual review.");}
        catch(org.springframework.dao.DataAccessException|jakarta.persistence.PersistenceException|org.springframework.transaction.TransactionException e){LOG.warn("Gallery cleanup reference check unavailable; files retained.");}
    }
}
