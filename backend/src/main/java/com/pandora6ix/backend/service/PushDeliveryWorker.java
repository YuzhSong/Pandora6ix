package com.pandora6ix.backend.service;

import static com.pandora6ix.backend.repository.BusinessDb.*;

import com.pandora6ix.backend.repository.BusinessDb;
import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PushDeliveryWorker {
    private static final Logger log = LoggerFactory.getLogger(PushDeliveryWorker.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final List<Duration> BACKOFF = List.of(
            Duration.ofSeconds(15), Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15));
    private static final String OWNED = "EXISTS (SELECT 1 FROM push_device p JOIN notification n ON n.enterprise_id=d.enterprise_id AND n.id=d.notification_id JOIN app_user u ON u.enterprise_id=n.enterprise_id AND u.id=n.user_id WHERE p.id=d.push_device_id AND p.active=TRUE AND p.enterprise_id=n.enterprise_id AND p.user_id=n.user_id AND u.status='ACTIVE')";
    private final BusinessDb db;
    private final JPushGateway gateway;

    public PushDeliveryWorker(BusinessDb db, JPushGateway gateway) {
        this.db = db;
        this.gateway = gateway;
    }

    @Scheduled(fixedDelayString = "${pandora.jpush.dispatch-delay-ms:10000}")
    public void dispatch() {
        if (!gateway.configured()) return;
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        db.update("UPDATE notification_push_delivery d SET status='CANCELLED',locked_until=NULL WHERE status IN ('PENDING','SENDING') AND NOT " + OWNED);
        db.update("UPDATE notification_push_delivery SET status='FAILED',locked_until=NULL WHERE attempt_count>=? AND ((status='PENDING' AND next_attempt_at<=?) OR (status='SENDING' AND locked_until<=?))", MAX_ATTEMPTS, now, now);
        var due = db.rows("SELECT d.id,d.notification_id,d.attempt_count,n.kind,n.event_type,n.title,p.registration_id "
                        + "FROM notification_push_delivery d JOIN notification n ON n.enterprise_id=d.enterprise_id AND n.id=d.notification_id "
                        + "JOIN push_device p ON p.id=d.push_device_id WHERE p.active=TRUE AND p.enterprise_id=n.enterprise_id AND p.user_id=n.user_id AND EXISTS (SELECT 1 FROM app_user u WHERE u.enterprise_id=n.enterprise_id AND u.id=n.user_id AND u.status='ACTIVE') AND d.attempt_count<5 AND ((d.status='PENDING' AND d.next_attempt_at<=?) "
                        + "OR (d.status='SENDING' AND d.locked_until<=?)) ORDER BY d.created_at,d.id LIMIT 50", now, now);
        for (var delivery : due) {
            if (Thread.currentThread().isInterrupted()) break;
            deliver(delivery, now);
        }
    }

    private void deliver(java.util.Map<String, Object> delivery, OffsetDateTime now) {
        String id = str(delivery, "id");
        OffsetDateTime lease = now.plusMinutes(2);
        int claimed = db.update("UPDATE notification_push_delivery d SET status='SENDING',attempt_count=attempt_count+1,locked_until=? "
                        + "WHERE id=? AND attempt_count=? AND " + OWNED + " AND ((status='PENDING' AND next_attempt_at<=?) OR (status='SENDING' AND locked_until<=?))",
                lease, id, delivery.get("attempt_count"), now, now);
        if (claimed == 0) return;
        int attempt = ((Number) delivery.get("attempt_count")).intValue() + 1;
        var current = db.optional("SELECT p.registration_id FROM notification_push_delivery d JOIN push_device p ON p.id=d.push_device_id WHERE d.id=? AND d.status='SENDING' AND d.attempt_count=? AND " + OWNED, id, attempt);
        if (current.isEmpty()) {
            db.update("UPDATE notification_push_delivery SET status='CANCELLED',locked_until=NULL WHERE id=? AND status='SENDING' AND attempt_count=?", id, attempt);
            return;
        }
        Integer status = null;
        try {
            status = gateway.send(str(current.get(), "registration_id"), str(delivery, "notification_id"),
                    str(delivery, "kind"), str(delivery, "event_type"), str(delivery, "title"));
            db.update("UPDATE notification_push_delivery SET status='DELIVERED',delivered_at=CURRENT_TIMESTAMP,locked_until=NULL,last_http_status=? WHERE id=? AND status='SENDING' AND attempt_count=?",
                    status, id, attempt);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            retry(id, attempt, null);
        } catch (IOException ex) {
            if (ex instanceof JPushGateway.PushRejectedException rejected) status = rejected.status();
            retry(id, attempt, status);
            log.warn("jpush_delivery_failed deliveryId={} httpStatus={} attempt={}", id,
                    status == null ? "unavailable" : status, attempt);
        }
    }

    private void retry(String id, int attempt, Integer status) {
        if (attempt >= MAX_ATTEMPTS) {
            db.update("UPDATE notification_push_delivery SET status='FAILED',locked_until=NULL,last_http_status=? WHERE id=? AND status='SENDING' AND attempt_count=?",
                    status, id, attempt);
            return;
        }
        Duration wait = BACKOFF.get(Math.min(attempt - 1, BACKOFF.size() - 1));
        db.update("UPDATE notification_push_delivery SET status='PENDING',next_attempt_at=?,locked_until=NULL,last_http_status=? WHERE id=? AND status='SENDING' AND attempt_count=?",
                OffsetDateTime.now(ZoneOffset.UTC).plus(wait), status, id, attempt);
    }
}
