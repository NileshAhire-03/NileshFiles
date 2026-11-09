package com.nilspath.api;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:rollback;DB_CLOSE_DELAY=-1","spring.datasource.username=sa",
    "spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver","spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop","app.admin.username=","app.admin.password="})
class InterviewTransactionTest {
    @Autowired Applications applications;
    @Autowired Interviews interviews;
    @Autowired InterviewService service;

    @Test void failureAfterInterviewInsertRollsBackBothChanges() {
        ApplicationRecord application=applications.saveAndFlush(new ApplicationRecord(1L,1L));
        long count=interviews.count();
        // Force the application update to fail after the interview insert succeeds.
        application.expectedSalary="x".repeat(256);
        try {
            assertThrows(RuntimeException.class,()->service.schedule(application,OffsetDateTime.now().plusDays(1).toString(),"Test"));
            assertEquals(count,interviews.count());
            assertEquals("APPLIED",applications.findById(application.id).orElseThrow().status);
        } finally { applications.deleteById(application.id); }
    }
}
