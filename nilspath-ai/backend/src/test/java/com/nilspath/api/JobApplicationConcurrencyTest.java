package com.nilspath.api;

import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:applications;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000","spring.datasource.username=sa",
    "spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver","spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop","app.admin.username=","app.admin.password="})
class JobApplicationConcurrencyTest {
    @Autowired Jobs jobs;
    @Autowired Applications applications;
    @Autowired JobApplicationService service;
    @Autowired PlatformTransactionManager transactions;

    Job fixture() { return jobs.saveAndFlush(new Job("Concurrent role","Company","Pune","Full Time","java",0,10L)); }

    @Test void applyingWaitsForClosureAndDoesNotSaveAgainstAClosedJob() throws Exception {
        Job job=fixture();
        var pool=Executors.newFixedThreadPool(2);
        var locked=new CountDownLatch(1);
        var release=new CountDownLatch(1);
        var started=new CountDownLatch(1);
        try {
            var closing=pool.submit(() -> new TransactionTemplate(transactions).execute(status -> {
                Job closingJob=jobs.findLockedById(job.id).orElseThrow();
                closingJob.active=false;
                jobs.saveAndFlush(closingJob);
                locked.countDown();
                try { if(!release.await(5,TimeUnit.SECONDS))throw new IllegalStateException("Test lock timed out"); }
                catch(InterruptedException ex) { Thread.currentThread().interrupt();throw new IllegalStateException(ex); }
                return null;
            }));
            assertTrue(locked.await(5,TimeUnit.SECONDS));
            var applying=pool.submit(() -> { started.countDown();return service.apply(job.id,20L,null); });
            assertTrue(started.await(5,TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,()->applying.get(300,TimeUnit.MILLISECONDS));
            release.countDown();
            closing.get(5,TimeUnit.SECONDS);
            ExecutionException failure=assertThrows(ExecutionException.class,()->applying.get(5,TimeUnit.SECONDS));
            assertInstanceOf(ResponseStatusException.class,failure.getCause());
            assertEquals(404,((ResponseStatusException)failure.getCause()).getStatusCode().value());
            assertFalse(applications.existsByJobIdAndCandidateId(job.id,20L));
        } finally {
            release.countDown();pool.shutdownNow();pool.awaitTermination(5,TimeUnit.SECONDS);
            applications.deleteAll(applications.findByJobId(job.id));jobs.deleteById(job.id);
        }
    }

    @Test void concurrentDuplicateApplicationsCreateOneRecord() throws Exception {
        Job job=fixture();
        var pool=Executors.newFixedThreadPool(2);
        var start=new CountDownLatch(1);
        Callable<Integer> apply=() -> {
            start.await();
            try { service.apply(job.id,30L,null);return 200; }
            catch(ResponseStatusException ex) { return ex.getStatusCode().value(); }
        };
        try {
            var first=pool.submit(apply);var second=pool.submit(apply);start.countDown();
            var outcomes=java.util.List.of(first.get(5,TimeUnit.SECONDS),second.get(5,TimeUnit.SECONDS));
            assertTrue(outcomes.contains(200));assertTrue(outcomes.contains(409));
            assertEquals(1,applications.findByJobId(job.id).size());
        } finally {
            start.countDown();pool.shutdownNow();pool.awaitTermination(5,TimeUnit.SECONDS);
            applications.deleteAll(applications.findByJobId(job.id));jobs.deleteById(job.id);
        }
    }
}
