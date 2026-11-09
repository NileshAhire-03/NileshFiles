package com.nilspath.api;

import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:companies;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000","spring.datasource.username=sa",
    "spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver","spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop","app.admin.username=","app.admin.password="})
class CompanyConcurrencyTest {
    @Autowired Companies companies;
    @Autowired PlatformService platform;
    @Autowired PlatformTransactionManager transactions;

    @Test void concurrentCompanyCreationReusesTheWinnerInsteadOfFailingTheJobPost() throws Exception {
        String name="Concurrent company";
        long before=companies.count();
        var pool=Executors.newFixedThreadPool(2);
        var inserted=new CountDownLatch(1);
        var release=new CountDownLatch(1);
        var started=new CountDownLatch(1);
        try {
            var winner=pool.submit(() -> new TransactionTemplate(transactions).execute(status -> {
                Company company=companies.saveAndFlush(new Company(name));
                inserted.countDown();
                try { if(!release.await(5,TimeUnit.SECONDS))throw new IllegalStateException("Test transaction timed out"); }
                catch(InterruptedException ex) {Thread.currentThread().interrupt();throw new IllegalStateException(ex);}
                return company.id;
            }));
            assertTrue(inserted.await(5,TimeUnit.SECONDS));
            var racing=pool.submit(() -> {started.countDown();return platform.ensureCompany(name);});
            assertTrue(started.await(5,TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,()->racing.get(300,TimeUnit.MILLISECONDS));
            release.countDown();
            Long id=winner.get(5,TimeUnit.SECONDS);
            assertEquals(id,racing.get(5,TimeUnit.SECONDS).id);
            assertEquals(before+1,companies.count());
        } finally {
            release.countDown();pool.shutdownNow();pool.awaitTermination(5,TimeUnit.SECONDS);
            companies.findByNameIgnoreCase(name).ifPresent(companies::delete);
        }
    }

    @Test void existingCompanyMetadataIsPreservedWhenPostingWithDifferentCaseAndSpaces() {
        Company original=new Company("Existing employer");
        original.city="Pune";original.website="https://example.com";
        original=companies.saveAndFlush(original);
        try {
            Company reused=platform.ensureCompany("  EXISTING EMPLOYER  ");
            assertEquals(original.id,reused.id);
            assertEquals("Existing employer",reused.name);
            assertEquals("Pune",reused.city);
            assertEquals("https://example.com",reused.website);
        } finally {companies.deleteById(original.id);}
    }
}
