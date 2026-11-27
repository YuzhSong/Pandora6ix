package com.pandora6ix.backend.crm;

import static com.pandora6ix.backend.crm.CrmAccess.*;
import static com.pandora6ix.backend.repository.BusinessDb.*;

import com.pandora6ix.backend.api.BusinessException;
import com.pandora6ix.backend.repository.BusinessDb;
import com.pandora6ix.backend.service.IdentityService.Actor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Profile("dev2")
class CrmCustomers {
    enum Kind { PERSON, COMPANY }

    record Input(@Size(max = 128) String name, @NotNull Kind kind,
                 @Size(max = 64) String category, @Size(max = 255) String contact,
                 @Size(max = 2000) String notes, @NotNull @Size(max = 36) String ownerUserId,
                 Long version) {}

    private static final String SELECT = """
            SELECT c.*,u.display_name AS owner_name FROM crm_customer c
            JOIN app_user u ON u.enterprise_id=c.enterprise_id AND u.id=c.owner_user_id
            """;
    private final BusinessDb db;
    private final CrmAccess access;

    CrmCustomers(BusinessDb db, CrmAccess access) {
        this.db = db;
        this.access = access;
    }

    List<Map<String, Object>> list(Actor actor, boolean mine, String search, Kind kind, int offset) {
        return access.ownerScope(actor, "c", mine, false).search("c.name", search)
                .filter("c.kind", kind)
                .rows(db, SELECT, " ORDER BY c.name,c.id OFFSET " + Math.max(0, offset)
                        + " ROWS FETCH NEXT 100 ROWS ONLY")
                .stream().map(BusinessDb::view).toList();
    }

    Map<String, Object> get(Actor actor, String id) {
        return view(access.ownerScope(actor, "c", false, false).and("c.id=?", id).one(db, SELECT));
    }

    @Transactional
    public Map<String, Object> save(Actor actor, String id, Input input) {
        access.requireManager(actor);
        String name = required(input.name(), "Customer name");
        if (id == null || !input.ownerUserId().equals(get(actor, id).get("ownerUserId"))) {
            access.requireActiveUser(actor, input.ownerUserId());
        }
        var category = optional(input.category());
        var contact = optional(input.contact());
        var notes = optional(input.notes());
        if (id == null) {
            id = id();
            db.update("""
                    INSERT INTO crm_customer(id,enterprise_id,owner_user_id,name,kind,category,contact,notes)
                    VALUES (?,?,?,?,?,?,?,?)
                    """, id, actor.enterpriseId(), input.ownerUserId(), name, input.kind().name(),
                    category, contact, notes);
            access.record(actor, id, "CUSTOMER", "CREATE", 0L);
        } else {
            get(actor, id);
            long version = expectedVersion(input.version());
            BusinessException.checkVersion(db.update("""
                    UPDATE crm_customer SET owner_user_id=?,name=?,kind=?,category=?,contact=?,notes=?,
                        version=version+1 WHERE enterprise_id=? AND id=? AND version=?
                    """, input.ownerUserId(), name, input.kind().name(), category, contact, notes,
                    actor.enterpriseId(), id, version));
            access.record(actor, id, "CUSTOMER", "UPDATE", version + 1);
        }
        return get(actor, id);
    }
}
