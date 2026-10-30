package com.pandora6ix.backend.service;

import com.pandora6ix.backend.repository.BusinessDb;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PushDeliveryWorkerTests {
    static class Gateway extends JPushGateway {
        boolean enabled = true, fail;
        int calls;
        Runnable duringSend;
        Gateway() { super("fixture-app-key", "fixture-secret", "http://127.0.0.1", JsonMapper.builder().build()); }
        @Override public boolean configured() { return enabled; }
        @Override public int send(String registration, String notification, String kind, String event, String title) throws IOException {
            calls++;
            assertEquals("registration", registration);
            if (duringSend != null) duringSend.run();
            if (fail) throw new IOException("fixture unavailable");
            return 200;
        }
    }
    static class Fixture implements AutoCloseable {
        final JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:push-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        final BusinessDb db = new BusinessDb(jdbc);
        final Gateway gateway = new Gateway();
        final PushDeliveryWorker worker = new PushDeliveryWorker(db, gateway);
        Fixture() {
            jdbc.execute("CREATE TABLE app_user(id VARCHAR,enterprise_id VARCHAR,status VARCHAR)");
            jdbc.execute("CREATE TABLE notification(id VARCHAR,enterprise_id VARCHAR,user_id VARCHAR,kind VARCHAR,event_type VARCHAR,title VARCHAR)");
            jdbc.execute("CREATE TABLE push_device(id VARCHAR,enterprise_id VARCHAR,user_id VARCHAR,registration_id VARCHAR,active BOOLEAN)");
            jdbc.execute("CREATE TABLE notification_push_delivery(id VARCHAR,enterprise_id VARCHAR,notification_id VARCHAR,push_device_id VARCHAR,status VARCHAR,attempt_count INT DEFAULT 0,next_attempt_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,locked_until TIMESTAMP WITH TIME ZONE,delivered_at TIMESTAMP WITH TIME ZONE,last_http_status INT,created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)");
            db.update("INSERT INTO app_user VALUES('user','company','ACTIVE')");
            db.update("INSERT INTO notification VALUES('notification','company','user','STATUS','REVIEW_RESULT','审批结果')");
            db.update("INSERT INTO push_device VALUES('device','company','user','registration',TRUE)");
            db.update("INSERT INTO notification_push_delivery(id,enterprise_id,notification_id,push_device_id,status) VALUES('delivery','company','notification','device','PENDING')");
            // Database microsecond rounding must not make a newly inserted fixture appear in the future.
            due();
        }
        String status() { return (String) db.one("SELECT status FROM notification_push_delivery").get("status"); }
        int attempts() { return ((Number) db.one("SELECT attempt_count FROM notification_push_delivery").get("attempt_count")).intValue(); }
        void due() { db.update("UPDATE notification_push_delivery SET next_attempt_at=DATEADD('SECOND',-1,CURRENT_TIMESTAMP),locked_until=DATEADD('SECOND',-1,CURRENT_TIMESTAMP)"); }
        @Override public void close() { jdbc.execute("SHUTDOWN"); }
    }

    @Test void absentConfigurationNeverCallsProvider() {
        try (var f = new Fixture()) {
            f.gateway.enabled = false; f.worker.dispatch();
            assertEquals("PENDING", f.status()); assertEquals(0, f.gateway.calls);
        }
    }
    @Test void successfulDeliveryIsNotRepeated() {
        try (var f = new Fixture()) {
            f.worker.dispatch(); f.worker.dispatch();
            assertEquals("DELIVERED", f.status()); assertEquals(1, f.gateway.calls); assertEquals(1, f.attempts());
        }
    }
    @Test void disabledAccountAndInactiveInstallationAreCancelled() {
        try (var f = new Fixture()) {
            f.db.update("UPDATE app_user SET status='DISABLED'"); f.worker.dispatch();
            assertEquals("CANCELLED", f.status()); assertEquals(0, f.gateway.calls);
        }
        try (var f = new Fixture()) {
            f.db.update("UPDATE push_device SET active=FALSE"); f.worker.dispatch();
            assertEquals("CANCELLED", f.status()); assertEquals(0, f.gateway.calls);
        }
    }
    @Test void reboundInstallationCannotReceivePreviousOwnersNotification() {
        try (var f = new Fixture()) {
            f.db.update("UPDATE push_device SET user_id='other',enterprise_id='other-company'");
            f.worker.dispatch();
            assertEquals("CANCELLED", f.status()); assertEquals(0, f.gateway.calls);
        }
    }
    @Test void failuresBackOffAndStopAfterFiveAttempts() {
        try (var f = new Fixture()) {
            f.gateway.fail = true;
            for (int i = 1; i <= 5; i++) {
                f.due(); f.worker.dispatch(); assertEquals(i, f.attempts());
                assertEquals(i == 5 ? "FAILED" : "PENDING", f.status());
                if (i < 5) {
                    f.worker.dispatch(); assertEquals(i, f.gateway.calls, "Backoff must defer immediate retries");
                }
            }
            f.due(); f.worker.dispatch(); assertEquals(5, f.gateway.calls);
        }
    }
    @Test void expiredLeaseRecoversButExhaustedLeaseDoesNotSendAgain() {
        try (var f = new Fixture()) {
            f.db.update("UPDATE notification_push_delivery SET status='SENDING',attempt_count=1"); f.due(); f.worker.dispatch();
            assertEquals("DELIVERED", f.status()); assertEquals(2, f.attempts());
        }
        try (var f = new Fixture()) {
            f.db.update("UPDATE notification_push_delivery SET status='SENDING',attempt_count=5"); f.due(); f.worker.dispatch();
            assertEquals("FAILED", f.status()); assertEquals(0, f.gateway.calls);
        }
    }
    @Test void staleSenderCannotFinishNewerAttempt() {
        try (var f = new Fixture()) {
            f.due();
            f.gateway.duringSend = () -> f.db.update("UPDATE notification_push_delivery SET attempt_count=2");
            f.worker.dispatch();
            assertEquals("SENDING", f.status()); assertEquals(2, f.attempts());
        }
    }
}
