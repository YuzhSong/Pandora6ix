package com.pandora6ix.backend.crm;

import static com.pandora6ix.backend.crm.CrmAccess.*;
import static com.pandora6ix.backend.repository.BusinessDb.*;

import com.pandora6ix.backend.api.BusinessException;
import com.pandora6ix.backend.repository.BusinessDb;
import com.pandora6ix.backend.service.IdentityService.Actor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Profile("dev2")
class CrmBusiness {
    enum Kind { MALL, PERFORMANCE, PRIVATE_DINING, VISIT, SALON, OTHER }
    enum Status { PLANNED, IN_PROGRESS, COMPLETED, CANCELLED }

    record Participant(@NotBlank @Size(max = 36) String userId,
                       @NotBlank @Size(max = 128) String responsibility) {}

    record Input(@NotBlank @Size(max = 160) String title, @NotNull Kind kind, @NotNull Status status,
                 @NotNull LocalDate eventDate, @Pattern(regexp = "(?:[01][0-9]|2[0-3]):[0-5][0-9]") String eventTime,
                 @NotBlank @Size(max = 36) String customerId, @NotBlank @Size(max = 36) String ownerUserId,
                 @Size(max = 2000) String notes, @NotNull @Size(max = 50) List<@NotNull @Valid Participant> participants,
                 Long version) {}

    private static final String SELECT = """
            SELECT b.*,c.name AS customer_name,u.display_name AS owner_name FROM crm_business b
            JOIN crm_customer c ON c.enterprise_id=b.enterprise_id AND c.id=b.customer_id
            JOIN app_user u ON u.enterprise_id=b.enterprise_id AND u.id=b.owner_user_id
            """;

    private final BusinessDb db;
    private final CrmAccess access;

    CrmBusiness(BusinessDb db, CrmAccess access) {
        this.db = db;
        this.access = access;
    }

    List<Map<String, Object>> list(Actor actor, boolean mine, String search, Kind kind,
            Status status, String customer, int offset) {
        var query = access.ownerScope(actor, "b", mine, true).search("b.title", search)
                .filter("b.kind", kind).filter("b.status", status);
        if (customer != null) query.and("b.customer_id=?", customer);
        return query.rows(db, SELECT, " ORDER BY b.event_date DESC,b.event_time DESC NULLS LAST,b.id OFFSET "
                + Math.max(0, offset) + " ROWS FETCH NEXT 100 ROWS ONLY").stream().map(BusinessDb::view).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(Actor actor, String id) {
        var business = view(access.ownerScope(actor, "b", false, true).and("b.id=?", id).one(db, SELECT));
        business.put("participants", participants(actor, id));
        return business;
    }

    private List<Map<String, Object>> participants(Actor actor, String id) {
        return db.rows("""
                SELECT p.user_id,u.display_name,p.responsibility FROM crm_participant p
                JOIN app_user u ON u.enterprise_id=p.enterprise_id AND u.id=p.user_id
                WHERE p.enterprise_id=? AND p.business_id=? ORDER BY u.display_name,p.user_id
                """, actor.enterpriseId(), id).stream().map(BusinessDb::view).toList();
    }

    @Transactional
    public Map<String, Object> save(Actor actor, String id, Input input) {
        access.requireManager(actor);
        if (db.optional("SELECT id FROM crm_customer WHERE enterprise_id=? AND id=?",
                actor.enterpriseId(), input.customerId()).isEmpty()) {
            throw BusinessException.invalid("Select a customer in your enterprise.");
        }
        var before = id == null ? null : db.lock("crm_business", actor.enterpriseId(), id);
        if (before == null || !input.ownerUserId().equals(str(before, "owner_user_id"))) {
            access.requireActiveUser(actor, input.ownerUserId());
        }
        Set<String> existing = id == null ? Set.of() : participants(actor, id).stream()
                .map(row -> str(row, "userId")).collect(Collectors.toSet());
        Set<String> selected = new HashSet<>();
        for (Participant participant : input.participants()) {
            if (input.ownerUserId().equals(participant.userId()) || !selected.add(participant.userId())) {
                throw BusinessException.invalid("A participant must be unique and different from the owner.");
            }
            if (!existing.contains(participant.userId())) access.requireActiveUser(actor, participant.userId());
        }
        String title = input.title().trim();
        String notes = optional(input.notes());
        long version = 0;
        if (id == null) {
            id = id();
            db.update("""
                    INSERT INTO crm_business(id,enterprise_id,customer_id,owner_user_id,title,kind,status,event_date,event_time,notes)
                    VALUES(?,?,?,?,?,?,?,?,?,?)
                    """, id, actor.enterpriseId(), input.customerId(), input.ownerUserId(), title, input.kind().name(),
                    input.status().name(), input.eventDate(), input.eventTime(), notes);
        } else {
            version = expectedVersion(input.version()) + 1;
            BusinessException.checkVersion(db.update("""
                    UPDATE crm_business SET customer_id=?,owner_user_id=?,title=?,kind=?,status=?,event_date=?,
                        event_time=?,notes=?,version=version+1 WHERE enterprise_id=? AND id=? AND version=?
                    """, input.customerId(), input.ownerUserId(), title, input.kind().name(), input.status().name(),
                    input.eventDate(), input.eventTime(), notes, actor.enterpriseId(), id, input.version()));
            db.update("DELETE FROM crm_participant WHERE enterprise_id=? AND business_id=?", actor.enterpriseId(), id);
        }
        for (Participant participant : input.participants()) {
            db.update("""
                    INSERT INTO crm_participant(enterprise_id,business_id,user_id,responsibility) VALUES(?,?,?,?)
                    """, actor.enterpriseId(), id, participant.userId(), participant.responsibility().trim());
        }
        access.record(actor, id, "BUSINESS", before == null ? "CREATE" : "UPDATE", version);
        return get(actor, id);
    }
}
