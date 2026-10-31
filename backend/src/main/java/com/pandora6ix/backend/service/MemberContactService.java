package com.pandora6ix.backend.service;

import static com.pandora6ix.backend.repository.BusinessDb.fields;

import com.pandora6ix.backend.api.BusinessException;
import com.pandora6ix.backend.repository.BusinessDb;
import org.springframework.stereotype.Service;

import java.util.Map;

/** Contact details are read individually, within the existing active management scope. */
@Service
public class MemberContactService {
    private final BusinessDb db;
    private final ScopeService scope;

    public MemberContactService(BusinessDb db, ScopeService scope) {
        this.db = db;
        this.scope = scope;
    }

    public Map<String, Object> contact(IdentityService.Actor actor, String id) {
        if (!scope.activeManaged(actor).contains(id)) throw BusinessException.missing();
        var row = db.optional(
                "SELECT id,phone_number FROM app_user WHERE enterprise_id=? AND id=? AND status='ACTIVE'",
                actor.enterpriseId(), id).orElseThrow(BusinessException::missing);
        return fields("id", row.get("id"), "phoneNumber", row.get("phone_number"));
    }
}
