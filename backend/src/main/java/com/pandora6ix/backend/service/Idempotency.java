package com.pandora6ix.backend.service;

import com.pandora6ix.backend.api.BusinessException;
import com.pandora6ix.backend.repository.BusinessDb;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.function.Supplier;

@Service
public class Idempotency {
    private final BusinessDb db;
    private final JsonMapper json;

    public Idempotency(BusinessDb db, JsonMapper json) {
        this.db = db;
        this.json = json;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Object execute(
            IdentityService.Actor actor,
            String operation,
            String key,
            Object request,
            Supplier<Object> command) {
        if (operation.equals("POST /company-important-items")) {
            if (!actor.has("ADMIN") && !actor.has("FOUNDER")) {
                if (actor.has("DEPARTMENT_MANAGER"))
                    throw BusinessException.policy(
                            "Restricted company publication scope is unconfirmed.");
                throw BusinessException.forbidden();
            }
        } else actor.business();
        if (key == null) return command.get();
        if (key.isBlank() || key.length() > 128)
            throw BusinessException.invalid("Idempotency-Key must contain 1–128 characters.");
        // Serializes competing first submissions for this actor, including rows not created yet.
        db.lock("app_user", actor.enterpriseId(), actor.id());
        String digest = IdentityService.hash(json.writeValueAsString(request));
        var existing =
                db.optional(
                        "SELECT * FROM api_idempotency WHERE enterprise_id=? AND actor_user_id=?"
                                + " AND operation=? AND idempotency_key=?",
                        actor.enterpriseId(),
                        actor.id(),
                        operation,
                        key);
        if (existing.isPresent()) {
            if (!digest.equals(existing.get().get("request_hash")))
                throw new BusinessException(
                        409,
                        "IDEMPOTENCY_CONFLICT",
                        "The key was already used for a different request.");
            return json.readValue((String) existing.get().get("response_json"), Map.class);
        }
        Object result = command.get();
        db.update(
                "INSERT INTO"
                    + " api_idempotency(enterprise_id,actor_user_id,operation,idempotency_key,request_hash,response_json)"
                    + " VALUES (?,?,?,?,?,?)",
                actor.enterpriseId(),
                actor.id(),
                operation,
                key,
                digest,
                json.writeValueAsString(result));
        return result;
    }
}
