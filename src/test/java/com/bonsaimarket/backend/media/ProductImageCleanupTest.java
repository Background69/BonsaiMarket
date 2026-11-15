package com.bonsaimarket.backend.media;

import jakarta.persistence.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.*;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Filesystem fixtures and mocked reference counts only. No database/runtime acceptance evidence. */
class ProductImageCleanupTest {
    @TempDir Path root;
    EntityManager em;ProductImageStorage storage;ProductImageCleanup cleanup;
    @SuppressWarnings("unchecked") @BeforeEach void setup(){
        Clock clock=Clock.fixed(Instant.ofEpochMilli(2000000),ZoneOffset.UTC);em=mock(EntityManager.class);
        var query=(TypedQuery<Long>)mock(TypedQuery.class,RETURNS_SELF);when(query.getSingleResult()).thenReturn(0L);when(em.createQuery(anyString(),eq(Long.class))).thenReturn(query);
        var manager=mock(PlatformTransactionManager.class);when(manager.getTransaction(any())).thenAnswer(invocation->new SimpleTransactionStatus());
        storage=new ProductImageStorage(root.toString(),5242880,clock);cleanup=new ProductImageCleanup(storage,em,clock,manager);
    }
    Path intent(String key)throws IOException{
        Files.createDirectories(storage.journalRoot());Files.write(root.resolve(key),new byte[]{1,2,3});
        Path path=storage.journalRoot().resolve(UUID.randomUUID()+".properties");var properties=new Properties();properties.setProperty("created","1");properties.setProperty("keys",key);
        try(var output=Files.newOutputStream(path)){properties.store(output,"fixture");}return path;
    }
    @Test void unreferencedFilesAndIntentAreRemovedAfterGrace()throws Exception{String key=UUID.randomUUID()+".png";var intent=intent(key);cleanup.process(intent);assertFalse(Files.exists(root.resolve(key)));assertFalse(Files.exists(intent));}
    @Test void activeTransactionLockPreventsCleanup()throws Exception{String key=UUID.randomUUID()+".png";var intent=intent(key);try(var channel=java.nio.channels.FileChannel.open(intent,StandardOpenOption.READ,StandardOpenOption.WRITE);var lock=channel.lock()){cleanup.process(intent);assertTrue(Files.exists(root.resolve(key)));assertTrue(Files.exists(intent));}}
    @Test void databaseFailureRetainsFileAndRetryIntent()throws Exception{String key=UUID.randomUUID()+".png";var intent=intent(key);when(em.createQuery(anyString(),eq(Long.class))).thenThrow(new PersistenceException("fixture unavailable"));cleanup.process(intent);assertTrue(Files.exists(root.resolve(key)));assertTrue(Files.exists(intent));}
    @SuppressWarnings("unchecked") @Test void referencedFileIsNeverDeleted()throws Exception{String key=UUID.randomUUID()+".png";var intent=intent(key);var query=(TypedQuery<Long>)mock(TypedQuery.class,RETURNS_SELF);when(query.getSingleResult()).thenReturn(1L);when(em.createQuery(anyString(),eq(Long.class))).thenReturn(query);cleanup.process(intent);assertTrue(Files.exists(root.resolve(key)));}
}
