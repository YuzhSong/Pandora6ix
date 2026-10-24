package com.pandora6ix.backend.service;

import static com.pandora6ix.backend.repository.BusinessDb.*;

import com.pandora6ix.backend.api.BusinessException;
import com.pandora6ix.backend.repository.BusinessDb;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Separate, bounded onboarding secrets; authentication continues to use BCrypt only. */
@Service
public class CredentialHandoffService {
    private final BusinessDb db;
    private final SecretKeySpec key;
    private final Duration retention;
    private final TransactionTemplate transactions;
    private final SecureRandom random = new SecureRandom();

    public CredentialHandoffService(BusinessDb db, PlatformTransactionManager manager,
            @Value("${pandora.handoff-key:${PANDORA_HANDOFF_KEY:}}") String configuredKey,
            @Value("${pandora.handoff-retention-hours:168}") long hours) {
        this.db = db;
        transactions = new TransactionTemplate(manager);
        if (hours < 1 || hours > 720) throw new IllegalArgumentException("Handoff retention must be 1-720 hours.");
        retention = Duration.ofHours(hours);
        if (configuredKey.isBlank()) key = null;
        else {
            byte[] bytes;
            try { bytes = Base64.getDecoder().decode(configuredKey); }
            catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Handoff key must be Base64 encoded."); }
            if (bytes.length != 32) throw new IllegalArgumentException("Handoff key must contain 32 bytes.");
            key = new SecretKeySpec(bytes, "AES");
            Arrays.fill(bytes, (byte) 0);
        }
    }

    public void capture(String enterprise, String user, String password, String passwordHash) {
        if (key == null) throw new BusinessException(503, "HANDOFF_KEY_NOT_CONFIGURED", "Configure the credential handoff encryption key.");
        String encrypted = encrypt(enterprise, user, password);
        db.update("DELETE FROM credential_handoff WHERE enterprise_id=? AND user_id=?", enterprise, user);
        db.update("INSERT INTO credential_handoff(enterprise_id,user_id,state,encrypted_password,hash_fingerprint,expires_at) VALUES(?,?,'AVAILABLE',?,?,?)",
                enterprise, user, encrypted, IdentityService.hash(passwordHash), OffsetDateTime.now(ZoneOffset.UTC).plus(retention));
    }

    public void changed(String enterprise, String user) {
        db.update("DELETE FROM credential_handoff WHERE enterprise_id=? AND user_id=?", enterprise, user);
        db.update("INSERT INTO credential_handoff(enterprise_id,user_id,state) VALUES(?,?,'CHANGED')", enterprise, user);
    }

    public void forget(String enterprise, String user) {
        db.update("DELETE FROM credential_handoff WHERE enterprise_id=? AND user_id=?", enterprise, user);
    }

    @Scheduled(fixedDelay = 60000)
    public void expire() {
        for (var enterprise : db.rows("SELECT DISTINCT enterprise_id FROM credential_handoff WHERE state='AVAILABLE'"))
            transactions.executeWithoutResult(ignored -> purge(str(enterprise, "enterprise_id")));
    }

    private void purge(String enterprise) {
        db.one("SELECT id FROM enterprise WHERE id=? FOR UPDATE", enterprise);
        db.update("UPDATE credential_handoff SET state='EXPIRED',encrypted_password=NULL,hash_fingerprint=NULL,expires_at=NULL WHERE enterprise_id=? AND state='AVAILABLE' AND expires_at<=CURRENT_TIMESTAMP", enterprise);
        for (var row : db.rows("SELECT h.*,u.password_hash FROM credential_handoff h JOIN app_user u ON u.enterprise_id=h.enterprise_id AND u.id=h.user_id WHERE h.enterprise_id=? AND h.state='AVAILABLE'", enterprise)) {
            if (!IdentityService.hash(str(row, "password_hash")).equals(str(row, "hash_fingerprint")))
                db.update("UPDATE credential_handoff SET state='CHANGED',encrypted_password=NULL,hash_fingerprint=NULL,expires_at=NULL WHERE enterprise_id=? AND user_id=? AND state='AVAILABLE' AND hash_fingerprint=? AND encrypted_password=?",
                        row.get("enterprise_id"), row.get("user_id"), row.get("hash_fingerprint"), row.get("encrypted_password"));
        }
    }

    @Transactional
    public Map<String, Object> read(IdentityService.Actor actor, String userId) {
        actor.admin();
        db.one("SELECT id FROM enterprise WHERE id=? FOR UPDATE", actor.enterpriseId());
        if (userId != null) {
            db.entity("app_user", actor.enterpriseId(), userId);
            if (isAdmin(actor.enterpriseId(), userId)) throw BusinessException.forbidden();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        String domain = str(db.one("SELECT login_domain FROM enterprise WHERE id=?", actor.enterpriseId()), "login_domain");
        var users = db.rows("SELECT u.* FROM app_user u WHERE u.enterprise_id=? AND NOT EXISTS(SELECT 1 FROM user_role r WHERE r.enterprise_id=u.enterprise_id AND r.user_id=u.id AND r.role_code='ADMIN')"
                + (userId == null ? "" : " AND u.id=?") + " ORDER BY u.id", userId == null ? new Object[]{actor.enterpriseId()} : new Object[]{actor.enterpriseId(), userId});
        for (var user : users) {
            String id = str(user, "id"), state = "UNAVAILABLE";
            String password = null, expiresAt = null;
            var optional = db.optional("SELECT * FROM credential_handoff WHERE enterprise_id=? AND user_id=?", actor.enterpriseId(), id);
            if (optional.isPresent()) {
                var row = optional.get();
                state = str(row, "state");
                if ("AVAILABLE".equals(state)) {
                    if (!IdentityService.hash(str(user, "password_hash")).equals(str(row, "hash_fingerprint"))) {
                        changed(actor.enterpriseId(), id); state = "CHANGED";
                    } else if (!((OffsetDateTime) row.get("expires_at")).toInstant().isAfter(Instant.now())) {
                        db.update("UPDATE credential_handoff SET state='EXPIRED',encrypted_password=NULL,hash_fingerprint=NULL,expires_at=NULL WHERE enterprise_id=? AND user_id=?", actor.enterpriseId(), id);
                        state = "EXPIRED";
                    } else if ("ACTIVE".equals(user.get("status"))) {
                        password = decrypt(actor.enterpriseId(), id, str(row, "encrypted_password"));
                        expiresAt = ((OffsetDateTime) row.get("expires_at")).toInstant().toString();
                    }
                }
            }
            if (!"ACTIVE".equals(user.get("status"))) state = "UNAVAILABLE";
            var item = fields("personId", id, "displayName", user.get("display_name"), "loginName", user.get("login_name"), "enterpriseDomain", domain, "status", state);
            if (password != null) { item.put("initialPassword", password); item.put("expiresAt", expiresAt); }
            result.add(item);
        }
        return fields("items", result, "nextCursor", null);
    }

    private boolean isAdmin(String enterprise, String user) {
        return !db.rows("SELECT user_id FROM user_role WHERE enterprise_id=? AND user_id=? AND role_code='ADMIN'", enterprise, user).isEmpty();
    }

    private byte[] aad(String enterprise, String user) { return (enterprise + ":" + user).getBytes(StandardCharsets.UTF_8); }

    private String encrypt(String enterprise, String user, String password) {
        byte[] plain = password.getBytes(StandardCharsets.UTF_8);
        try {
            byte[] nonce = new byte[12]; random.nextBytes(nonce);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce)); cipher.updateAAD(aad(enterprise, user));
            byte[] encrypted = cipher.doFinal(plain);
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + encrypted.length).put(nonce).put(encrypted).array());
        } catch (GeneralSecurityException ex) { throw unavailable(); }
        finally { Arrays.fill(plain, (byte) 0); }
    }

    private String decrypt(String enterprise, String user, String encrypted) {
        if (key == null) throw unavailable();
        byte[] plain = null;
        try {
            byte[] bytes = Base64.getDecoder().decode(encrypted);
            if (bytes.length < 29) throw unavailable();
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, Arrays.copyOf(bytes, 12))); cipher.updateAAD(aad(enterprise, user));
            plain = cipher.doFinal(bytes, 12, bytes.length - 12);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) { throw unavailable(); }
        finally { if (plain != null) Arrays.fill(plain, (byte) 0); }
    }

    private BusinessException unavailable() { return new BusinessException(503, "HANDOFF_UNAVAILABLE", "Credential handoff is unavailable."); }
}
