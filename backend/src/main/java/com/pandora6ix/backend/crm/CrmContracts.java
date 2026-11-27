package com.pandora6ix.backend.crm;

import static com.pandora6ix.backend.crm.CrmAccess.*;
import static com.pandora6ix.backend.repository.BusinessDb.*;

import com.pandora6ix.backend.api.BusinessException;
import com.pandora6ix.backend.repository.BusinessDb;
import com.pandora6ix.backend.service.IdentityService;
import com.pandora6ix.backend.service.IdentityService.Actor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Profile("dev2")
class CrmContracts {
    private static final int NOTICE_DAYS = 30;

    enum Expiry { UPCOMING, ACTIVE, EXPIRING, EXPIRED }

    record Input(@NotBlank @Size(max = 36) String employeeUserId, @NotBlank @Size(max = 128) String title,
                 @NotNull LocalDate startDate, @NotNull LocalDate endDate, @Size(max = 2000) String notes,
                 Long version) {}

    record Download(String name, byte[] bytes) {}

    private static final String SELECT = """
            SELECT c.*,u.display_name AS employee_name FROM crm_employment_contract c
            JOIN app_user u ON u.enterprise_id=c.enterprise_id AND u.id=c.employee_user_id
            """;
    private final BusinessDb db;
    private final CrmAccess access;
    private final IdentityService identities;
    private final CrmAttachments files;

    CrmContracts(BusinessDb db, CrmAccess access, IdentityService identities, CrmAttachments files) {
        this.db = db;
        this.access = access;
        this.identities = identities;
        this.files = files;
    }

    private Query scope(Actor actor) {
        var query = new Query("c.enterprise_id=?", actor.enterpriseId());
        return actor.has("ADMIN") ? query : query.and("c.employee_user_id=?", actor.id());
    }

    List<Map<String, Object>> list(Actor actor, String search, Expiry expiry, int offset) {
        var today = identities.today(actor);
        var deadline = today.plusDays(NOTICE_DAYS);
        var query = scope(actor).search("u.display_name", search);
        if (expiry != null) {
            switch (expiry) {
                case EXPIRED -> query.and("c.end_date < ?", today);
                case EXPIRING -> query.and("c.end_date BETWEEN ? AND ?", today, deadline);
                case UPCOMING -> query.and("c.start_date > ? AND c.end_date > ?", today, deadline);
                case ACTIVE -> query.and("c.start_date <= ? AND c.end_date > ?", today, deadline);
            }
        }
        return query.rows(db, SELECT, " ORDER BY c.end_date,c.id OFFSET " + Math.max(0, offset)
                + " ROWS FETCH NEXT 100 ROWS ONLY").stream().map(row -> contractView(row, today)).toList();
    }

    Map<String, Object> get(Actor actor, String id) {
        return contractView(scope(actor).and("c.id=?", id).one(db, SELECT), identities.today(actor));
    }

    private Map<String, Object> contractView(Map<String, Object> row, LocalDate today) {
        var contract = view(row);
        contract.remove("attachmentId");
        var end = LocalDate.parse(str(row, "end_date"));
        var start = LocalDate.parse(str(row, "start_date"));
        Expiry expiry;
        if (end.isBefore(today)) expiry = Expiry.EXPIRED;
        else if (!end.isAfter(today.plusDays(NOTICE_DAYS))) expiry = Expiry.EXPIRING;
        else if (start.isAfter(today)) expiry = Expiry.UPCOMING;
        else expiry = Expiry.ACTIVE;
        contract.put("expiry", expiry.name());
        return contract;
    }

    @Transactional
    public Map<String, Object> save(Actor actor, String id, Input input) {
        actor.admin();
        if (input.endDate().isBefore(input.startDate())) {
            throw BusinessException.invalid("End date must not precede start date.");
        }
        if (id == null) {
            db.entity("app_user", actor.enterpriseId(), input.employeeUserId());
            id = id();
            db.update("""
                    INSERT INTO crm_employment_contract(id,enterprise_id,employee_user_id,title,start_date,end_date,notes)
                    VALUES(?,?,?,?,?,?,?)
                    """, id, actor.enterpriseId(), input.employeeUserId(), input.title().trim(), input.startDate(),
                    input.endDate(), optional(input.notes()));
            access.record(actor, id, "EMPLOYMENT_CONTRACT", "CREATE", 0L);
        } else {
            var row = db.entity("crm_employment_contract", actor.enterpriseId(), id);
            if (!input.employeeUserId().equals(str(row, "employee_user_id"))) {
                throw BusinessException.invalid("The contract employee is fixed.");
            }
            long version = expectedVersion(input.version());
            BusinessException.checkVersion(db.update("""
                    UPDATE crm_employment_contract SET title=?,start_date=?,end_date=?,notes=?,version=version+1
                    WHERE enterprise_id=? AND id=? AND version=?
                    """, input.title().trim(), input.startDate(), input.endDate(), optional(input.notes()),
                    actor.enterpriseId(), id, version));
            access.record(actor, id, "EMPLOYMENT_CONTRACT", "UPDATE", version + 1);
        }
        return get(actor, id);
    }

    @Transactional
    public Map<String, Object> upload(Actor actor, String id, long requestedVersion, MultipartFile file) {
        actor.admin();
        var row = db.lock("crm_employment_contract", actor.enterpriseId(), id);
        BusinessException.checkVersion(requestedVersion == number(row, "version") ? 1 : 0);
        var saved = files.replace(file, str(row, "attachment_id"));
        BusinessException.checkVersion(db.update("""
                UPDATE crm_employment_contract SET attachment_id=?,attachment_name=?,version=version+1
                WHERE enterprise_id=? AND id=? AND version=?
                """, saved.id(), saved.name(), actor.enterpriseId(), id, requestedVersion));
        access.record(actor, id, "EMPLOYMENT_CONTRACT", "UPLOAD_ATTACHMENT", requestedVersion + 1);
        return get(actor, id);
    }

    @Transactional
    public Download download(Actor actor, String id) {
        // Authorize before touching storage. Hold the row until the current PDF has been read.
        var row = db.lock("crm_employment_contract", actor.enterpriseId(), id);
        if (!actor.has("ADMIN") && !actor.id().equals(str(row, "employee_user_id"))) {
            throw BusinessException.missing();
        }
        return new Download(str(row, "attachment_name"), files.read(str(row, "attachment_id")));
    }
}
