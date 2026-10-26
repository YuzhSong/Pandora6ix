package com.pandora6ix.backend.service;

import static com.pandora6ix.backend.repository.BusinessDb.*;

import com.pandora6ix.backend.api.BusinessException;
import com.pandora6ix.backend.api.Contracts;
import com.pandora6ix.backend.repository.BusinessDb;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushDeviceService {
    private final BusinessDb db;

    public PushDeviceService(BusinessDb db) {
        this.db = db;
    }

    @Transactional
    public Object register(IdentityService.Actor actor, Contracts.PushDeviceRegistration command) {
        var existing = db.optional(
                "SELECT * FROM push_device WHERE installation_id=? FOR UPDATE",
                command.installationId());
        if (existing.isPresent()) {
            String deviceId = str(existing.get(), "id");
            if (!actor.enterpriseId().equals(str(existing.get(), "enterprise_id"))
                    || !actor.id().equals(str(existing.get(), "user_id")))
                db.update("UPDATE notification_push_delivery SET status='CANCELLED' WHERE push_device_id=? AND status IN ('PENDING','SENDING')",
                        deviceId);
            db.update("UPDATE push_device SET enterprise_id=?,user_id=?,registration_id=?,active=TRUE,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                    actor.enterpriseId(), actor.id(), command.registrationId(), deviceId);
        } else {
            db.update("INSERT INTO push_device(id,enterprise_id,user_id,installation_id,registration_id) VALUES(?,?,?,?,?)",
                    id(), actor.enterpriseId(), actor.id(), command.installationId(), command.registrationId());
        }
        return fields("installationId", command.installationId(), "active", true);
    }

    @Transactional
    public Object deactivate(IdentityService.Actor actor, String installationId) {
        int changed = db.update("UPDATE push_device SET active=FALSE,updated_at=CURRENT_TIMESTAMP WHERE enterprise_id=? AND user_id=? AND installation_id=? AND active=TRUE",
                actor.enterpriseId(), actor.id(), installationId);
        if (changed == 0 && db.optional("SELECT id FROM push_device WHERE enterprise_id=? AND user_id=? AND installation_id=?",
                actor.enterpriseId(), actor.id(), installationId).isEmpty()) throw BusinessException.missing();
        db.update("UPDATE notification_push_delivery SET status='CANCELLED' WHERE push_device_id IN (SELECT id FROM push_device WHERE enterprise_id=? AND user_id=? AND installation_id=?) AND status IN ('PENDING','SENDING')",
                actor.enterpriseId(), actor.id(), installationId);
        return fields("installationId", installationId, "active", false);
    }
}
