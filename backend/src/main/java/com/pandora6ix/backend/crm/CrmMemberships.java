package com.pandora6ix.backend.crm;

import static com.pandora6ix.backend.crm.CrmAccess.*;
import static com.pandora6ix.backend.repository.BusinessDb.*;

import com.pandora6ix.backend.api.BusinessException;
import com.pandora6ix.backend.repository.BusinessDb;
import com.pandora6ix.backend.service.IdentityService.Actor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Profile("dev2")
class CrmMemberships {
    enum UseStatus { SCHEDULED, COMPLETED, CANCELLED }

    record MembershipInput(@NotBlank @Size(max = 36) String customerId,
                           @NotBlank @Size(max = 80) String membershipType,
                           @NotNull LocalDate startDate, @NotNull LocalDate endDate, Long version) {}

    record BenefitInput(@NotBlank @Size(max = 128) String name, @Size(max = 1000) String notes, Long version) {}

    record UseInput(@NotNull LocalDate serviceDate, @NotNull UseStatus status,
                    @Size(max = 1000) String notes, Long version) {}

    private static final String MEMBERSHIPS = """
            SELECT m.*,c.name AS customer_name FROM crm_membership m
            JOIN crm_customer c ON c.enterprise_id=m.enterprise_id AND c.id=m.customer_id
            """;
    private final BusinessDb db;
    private final CrmAccess access;
    private final CrmCustomers customers;

    CrmMemberships(BusinessDb db, CrmAccess access, CrmCustomers customers) {
        this.db = db;
        this.access = access;
        this.customers = customers;
    }

    List<Map<String, Object>> list(Actor actor, String search, String customerId, int offset) {
        var query = access.ownerScope(actor, "c", false, false).search("c.name", search);
        if (customerId != null) query.and("m.customer_id=?", customerId);
        return query.rows(db, MEMBERSHIPS, " ORDER BY m.end_date DESC,m.id OFFSET " + Math.max(0, offset)
                + " ROWS FETCH NEXT 100 ROWS ONLY").stream().map(BusinessDb::view).toList();
    }

    Map<String, Object> get(Actor actor, String id) {
        return view(access.ownerScope(actor, "c", false, false).and("m.id=?", id).one(db, MEMBERSHIPS));
    }

    List<Map<String, Object>> benefits(Actor actor, String membership, int offset) {
        var query = access.ownerScope(actor, "c", false, false).and("m.id=?", membership);
        get(actor, membership);
        return query.rows(db, """
                SELECT b.* FROM crm_benefit b
                JOIN crm_membership m ON m.enterprise_id=b.enterprise_id AND m.id=b.membership_id
                JOIN crm_customer c ON c.enterprise_id=m.enterprise_id AND c.id=m.customer_id
                """, " ORDER BY b.name,b.id OFFSET " + Math.max(0, offset)
                + " ROWS FETCH NEXT 100 ROWS ONLY").stream().map(BusinessDb::view).toList();
    }

    List<Map<String, Object>> uses(Actor actor, String benefit, int offset) {
        var query = access.ownerScope(actor, "c", false, false).and("b.id=?", benefit);
        query.one(db, """
                SELECT b.id FROM crm_benefit b
                JOIN crm_membership m ON m.enterprise_id=b.enterprise_id AND m.id=b.membership_id
                JOIN crm_customer c ON c.enterprise_id=m.enterprise_id AND c.id=m.customer_id
                """);
        return query.rows(db, """
                SELECT u.* FROM crm_benefit_use u
                JOIN crm_benefit b ON b.enterprise_id=u.enterprise_id AND b.id=u.benefit_id
                JOIN crm_membership m ON m.enterprise_id=b.enterprise_id AND m.id=b.membership_id
                JOIN crm_customer c ON c.enterprise_id=m.enterprise_id AND c.id=m.customer_id
                """, " ORDER BY u.service_date DESC,u.id OFFSET " + Math.max(0, offset)
                + " ROWS FETCH NEXT 100 ROWS ONLY").stream().map(BusinessDb::view).toList();
    }

    @Transactional
    public Map<String, Object> saveMembership(Actor actor, String id, MembershipInput input) {
        access.requireManager(actor);
        customers.get(actor, input.customerId());
        if (input.endDate().isBefore(input.startDate())) {
            throw BusinessException.invalid("End date must not precede start date.");
        }
        if (id == null) {
            id = id();
            db.update("""
                    INSERT INTO crm_membership(id,enterprise_id,customer_id,membership_type,start_date,end_date)
                    VALUES (?,?,?,?,?,?)
                    """, id, actor.enterpriseId(), input.customerId(), input.membershipType().trim(),
                    input.startDate(), input.endDate());
            access.record(actor, id, "MEMBERSHIP", "CREATE", 0L);
        } else {
            var existing = db.entity("crm_membership", actor.enterpriseId(), id);
            if (!input.customerId().equals(str(existing, "customer_id"))) {
                throw BusinessException.invalid("The membership customer is fixed.");
            }
            long version = expectedVersion(input.version());
            BusinessException.checkVersion(db.update("""
                    UPDATE crm_membership SET membership_type=?,start_date=?,end_date=?,version=version+1
                    WHERE enterprise_id=? AND id=? AND version=?
                    """, input.membershipType().trim(), input.startDate(), input.endDate(),
                    actor.enterpriseId(), id, version));
            access.record(actor, id, "MEMBERSHIP", "UPDATE", version + 1);
        }
        return get(actor, id);
    }

    @Transactional
    public Map<String, Object> saveBenefit(Actor actor, String membership, String id, BenefitInput input) {
        access.requireManager(actor);
        db.entity("crm_membership", actor.enterpriseId(), membership);
        if (id == null) {
            id = id();
            db.update("""
                    INSERT INTO crm_benefit(id,enterprise_id,membership_id,name,notes) VALUES (?,?,?,?,?)
                    """, id, actor.enterpriseId(), membership, input.name().trim(), optional(input.notes()));
            access.record(actor, id, "BENEFIT", "CREATE", 0L);
        } else {
            db.one("SELECT id FROM crm_benefit WHERE enterprise_id=? AND membership_id=? AND id=?",
                    actor.enterpriseId(), membership, id);
            long version = expectedVersion(input.version());
            BusinessException.checkVersion(db.update("""
                    UPDATE crm_benefit SET name=?,notes=?,version=version+1
                    WHERE enterprise_id=? AND membership_id=? AND id=? AND version=?
                    """, input.name().trim(), optional(input.notes()), actor.enterpriseId(), membership, id, version));
            access.record(actor, id, "BENEFIT", "UPDATE", version + 1);
        }
        return view(db.entity("crm_benefit", actor.enterpriseId(), id));
    }

    @Transactional
    public Map<String, Object> saveUse(Actor actor, String benefit, String id, UseInput input) {
        access.requireManager(actor);
        db.entity("crm_benefit", actor.enterpriseId(), benefit);
        if (id == null) {
            id = id();
            db.update("""
                    INSERT INTO crm_benefit_use(id,enterprise_id,benefit_id,service_date,status,notes)
                    VALUES (?,?,?,?,?,?)
                    """, id, actor.enterpriseId(), benefit, input.serviceDate(), input.status().name(), optional(input.notes()));
            access.record(actor, id, "BENEFIT_USE", "CREATE", 0L);
        } else {
            db.one("SELECT id FROM crm_benefit_use WHERE enterprise_id=? AND benefit_id=? AND id=?",
                    actor.enterpriseId(), benefit, id);
            long version = expectedVersion(input.version());
            BusinessException.checkVersion(db.update("""
                    UPDATE crm_benefit_use SET service_date=?,status=?,notes=?,version=version+1
                    WHERE enterprise_id=? AND benefit_id=? AND id=? AND version=?
                    """, input.serviceDate(), input.status().name(), optional(input.notes()),
                    actor.enterpriseId(), benefit, id, version));
            access.record(actor, id, "BENEFIT_USE", "UPDATE", version + 1);
        }
        return view(db.entity("crm_benefit_use", actor.enterpriseId(), id));
    }
}
