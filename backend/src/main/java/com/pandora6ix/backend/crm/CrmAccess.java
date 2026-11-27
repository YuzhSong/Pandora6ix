package com.pandora6ix.backend.crm;

import com.pandora6ix.backend.api.BusinessException;
import com.pandora6ix.backend.repository.BusinessDb;
import com.pandora6ix.backend.service.IdentityService.Actor;
import com.pandora6ix.backend.service.MutationAudit;
import com.pandora6ix.backend.service.ScopeService;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Profile("dev2")
class CrmAccess {
    private final BusinessDb db;
    private final ScopeService scope;
    private final MutationAudit audit;

    CrmAccess(BusinessDb db, ScopeService scope, MutationAudit audit) {
        this.db = db;
        this.scope = scope;
        this.audit = audit;
    }

    boolean canManage(Actor actor) {
        return actor.has("ADMIN") || actor.has("FOUNDER");
    }

    void requireManager(Actor actor) {
        if (!canManage(actor)) throw BusinessException.forbidden();
    }

    void requireActiveUser(Actor actor, String id) {
        if (db.optional("""
                SELECT id FROM app_user
                WHERE enterprise_id=? AND id=? AND status='ACTIVE' AND employment_status='IN_SERVICE'
                """, actor.enterpriseId(), id).isEmpty()) {
            throw BusinessException.invalid("Select an active employee in your enterprise.");
        }
    }

    List<Map<String, Object>> people(Actor actor) {
        requireManager(actor);
        return db.rows("""
                SELECT id,display_name FROM app_user
                WHERE enterprise_id=? AND status='ACTIVE' AND employment_status='IN_SERVICE'
                ORDER BY display_name,id
                """, actor.enterpriseId()).stream().map(BusinessDb::view).toList();
    }

    Query ownerScope(Actor actor, String alias, boolean mine, boolean business) {
        var query = new Query(alias + ".enterprise_id=?", actor.enterpriseId());
        if (!mine && canManage(actor)) return query;
        Set<String> owners = new LinkedHashSet<>();
        owners.add(actor.id());
        if (!mine && actor.businessRank() < 3) owners.addAll(scope.managed(actor));
        String ownerFilter = alias + ".owner_user_id IN ("
                + String.join(",", Collections.nCopies(owners.size(), "?")) + ")";
        if (business) {
            var args = new ArrayList<>(owners);
            args.add(actor.id());
            return query.and("(" + ownerFilter + """
                     OR EXISTS (SELECT 1 FROM crm_participant p
                     WHERE p.enterprise_id=b.enterprise_id AND p.business_id=b.id AND p.user_id=?))
                    """, args.toArray());
        }
        return query.and(ownerFilter, owners.toArray());
    }

    void record(Actor actor, String id, String type, String action, Long version) {
        Map<String, Object> after = version == null ? null : Map.of("version", version);
        audit.record(actor, id, type, "CRM", action, "CRM " + type + " " + action, null, after);
    }

    static long expectedVersion(Long version) {
        if (version == null || version < 0) throw BusinessException.invalid("A valid version is required.");
        return version;
    }

    static String required(String value, String field) {
        if (value == null || value.isBlank()) throw BusinessException.invalid(field + " is required.");
        return value.trim();
    }

    static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static class Query {
        private final StringBuilder sql = new StringBuilder();
        private final List<Object> args = new ArrayList<>();

        Query(String clause, Object... values) {
            sql.append(clause);
            Collections.addAll(args, values);
        }

        Query and(String clause, Object... values) {
            sql.append(" AND ").append(clause);
            Collections.addAll(args, values);
            return this;
        }

        Query search(String column, String value) {
            return value == null || value.isBlank() ? this
                    : and("LOCATE(LOWER(?), LOWER(" + column + ")) > 0", value.trim());
        }

        Query filter(String column, Enum<?> value) {
            return value == null ? this : and(column + "=?", value.name());
        }

        List<Map<String, Object>> rows(BusinessDb db, String select, String suffix) {
            return db.rows(select + " WHERE " + sql + suffix, args.toArray());
        }

        Map<String, Object> one(BusinessDb db, String select) {
            return db.one(select + " WHERE " + sql, args.toArray());
        }
    }
}
