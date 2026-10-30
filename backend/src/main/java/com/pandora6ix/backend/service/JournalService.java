package com.pandora6ix.backend.service;

import static com.pandora6ix.backend.api.BusinessException.checkVersion;
import static com.pandora6ix.backend.repository.BusinessDb.*;

import com.pandora6ix.backend.api.*;
import com.pandora6ix.backend.repository.BusinessDb;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class JournalService implements com.pandora6ix.backend.application.journals.JournalUseCases {
    private final MutationAudit audit;
    private final BusinessDb db;
    private final IdentityService identity;
    private final ScopeService scope;
    private final TagService tags;

    public JournalService(
            BusinessDb db,
            MutationAudit audit, IdentityService identity, ScopeService scope, TagService tags) {
        this.audit = audit;
        this.db = db;
        this.identity = identity;
        this.scope = scope;
        this.tags = tags;
    }

    public Map<String, Object> list(
            IdentityService.Actor a,
            LocalDate date,
            LocalDate from,
            LocalDate to,
            String requestedScope,
            String cursor,
            int limit) {
        a.business();
        if (!Set.of("mine", "managed").contains(requestedScope))
            throw BusinessException.invalid("Journal scope must be mine or managed.");
        if (date != null && (from != null || to != null))
            throw BusinessException.invalid("Use either date or from/to.");
        if (from != null && to != null && (to.isBefore(from) || to.isAfter(from.plusDays(366))))
            throw BusinessException.invalid(
                    "Query one window of at most 367 days; older windows remain available.");
        Set<String> authors = "managed".equals(requestedScope) ? scope.managed(a) : Set.of(a.id());
        StringBuilder sql = new StringBuilder("SELECT * FROM work_log WHERE enterprise_id=?");
        List<Object> args = new ArrayList<>(List.of(a.enterpriseId()));
        if (authors.isEmpty()) sql.append(" AND 1=0");
        else {
            sql.append(" AND author_user_id IN (").append(Pages.placeholders(authors)).append(")");
            args.addAll(authors);
        }
        if (date != null) {
            sql.append(" AND work_date=?");
            args.add(date);
        }
        if (from != null) {
            sql.append(" AND work_date>=?");
            args.add(from);
        }
        if (to != null) {
            sql.append(" AND work_date<=?");
            args.add(to);
        }
        return Pages.sqlPage(
                db,
                sql.toString(),
                args,
                cursor,
                limit,
                a.enterpriseId()
                        + ":"
                        + a.id()
                        + ":journals:"
                        + requestedScope
                        + ":"
                        + date
                        + ":"
                        + from
                        + ":"
                        + to,
                List.of("work_date", "published_at", "id"),
                true,
                row -> journalView(a, row));
    }

    private Map<String, Object> journalView(IdentityService.Actor a, Map<String, Object> journal) {
        var result = view(journal);
        result.put("journalId", result.get("id"));
        var entries =
                db
                        .rows(
                                "SELECT * FROM work_log_entry WHERE enterprise_id=? AND"
                                        + " work_log_id=? ORDER BY display_order,id",
                                a.enterpriseId(),
                                journal.get("id"))
                        .stream()
                        .map(
                                r -> {
                                    var entry = view(r);
                                    entry.put("entryId", entry.get("id"));
                                    return entry;
                                })
                        .toList();
        result.put("entries", entries);
        var linked = tags.linked(a, "work_log_tag", "work_log_id", str(journal, "id"));
        result.put("tags", linked);
        result.put("tagIds", linked.stream().map(t -> t.get("id")).toList());
        result.put("businessToday", identity.today(a).toString());
        result.put("allowedActions", a.id().equals(journal.get("author_user_id")) && createdToday(a, journal)
                ? List.of("EDIT", "DELETE") : List.of());
        return result;
    }

    /**
     * 创建当天可编辑：以 published_at（创建时间）的业务日期判断，而不是 work_date。
     * 这样当天补交的往期日志在当天内仍可修改，跨天后转为只读。
     */
    private boolean createdToday(IdentityService.Actor a, Map<String, Object> row) {
        var zone = identity.zone(a);
        var today = identity.today(a);
        Object value = row.get("published_at");
        if (value instanceof java.time.OffsetDateTime odt)
            return odt.atZoneSameInstant(zone).toLocalDate().equals(today);
        if (value instanceof java.time.Instant instant)
            return instant.atZone(zone).toLocalDate().equals(today);
        return false;
    }

    /**
     * 有日志的日期（本人、全部历史）：日期选择器要按「有日志 / 无日志 / 今天」着色，
     * 且月份必须连续展示，因此不受列表接口 367 天窗口限制，只返回日期本身。
     */
    public Map<String, Object> dates(IdentityService.Actor a) {
        a.business();
        var items =
                db.rows(
                                "SELECT DISTINCT work_date FROM work_log WHERE enterprise_id=? AND"
                                        + " author_user_id=? ORDER BY work_date",
                                a.enterpriseId(),
                                a.id())
                        .stream()
                        .map(row -> str(row, "work_date"))
                        .toList();
        return fields("items", items, "businessToday", identity.today(a).toString());
    }

    public Map<String, Object> detail(IdentityService.Actor a, String id) {
        a.business();
        var row = db.entity("work_log", a.enterpriseId(), id);
        scope.read(a, str(row, "author_user_id"));
        return journalView(a, row);
    }

    public Map<String, Object> entry(IdentityService.Actor a, String id) {
        a.business();
        var row =
                db.one(
                        "SELECT e.*,j.work_date FROM work_log_entry e JOIN work_log j ON"
                                + " j.enterprise_id=e.enterprise_id AND j.id=e.work_log_id WHERE"
                                + " e.enterprise_id=? AND e.id=?",
                        a.enterpriseId(),
                        id);
        scope.read(a, str(row, "author_user_id"));
        return entryView(row);
    }

    private Map<String, Object> entryView(Map<String, Object> row) {
        var result = view(row);
        result.put("entryId", result.get("id"));
        result.put("journalId", result.remove("workLogId"));
        return result;
    }

    @Transactional
    public Map<String, Object> create(IdentityService.Actor a, Contracts.JournalCreate c) {
        a.business();
        LocalDate today = identity.today(a);
        if (c.workDate().isBefore(today.minusDays(2)) || c.workDate().isAfter(today))
            throw new BusinessException(422, "JOURNAL_DATE_OUT_OF_RANGE",
                    "Journals can only be created for today and the previous two business dates.");
        db.lock("app_user", a.enterpriseId(), a.id());
        var duplicate =
                db.optional(
                        "SELECT id FROM work_log WHERE enterprise_id=? AND author_user_id=? AND"
                                + " work_date=?",
                        a.enterpriseId(),
                        a.id(),
                        c.workDate());
        if (duplicate.isPresent())
            throw new BusinessException(
                    409,
                    "DAILY_JOURNAL_EXISTS",
                    "Edit the existing journal for this business date.",
                    Map.of("existingJournalId", duplicate.get().get("id")));
        var tagIds = tags.validate(a, c.tagIds(), c.primaryTagId());
        Set<String> keys = new HashSet<>();
        for (var entry : c.entries())
            if (!keys.add(entry.clientEntryKey()))
                throw BusinessException.invalid("clientEntryKey must be unique within a journal.");
        String id = id();
        db.update(
                "INSERT INTO work_log(id,enterprise_id,author_user_id,work_date,primary_tag_id)"
                        + " VALUES(?,?,?,?,?)",
                id,
                a.enterpriseId(),
                a.id(),
                c.workDate(),
                c.primaryTagId());
        int order = 0;
        for (var entry : c.entries())
            insertEntry(a, id, entry.clientEntryKey(), entry.content(), order++);
        tags.replace(a, "work_log_tag", "work_log_id", id, tagIds);
        audit.record(a,id,"WORK_LOG","WORK_LOG","CREATE","新增工作日报",null,audit.journal(a,id));
        return detail(a, id);
    }

    private void insertEntry(
            IdentityService.Actor a, String journal, String key, String content, int order) {
        db.update(
                "INSERT INTO"
                    + " work_log_entry(id,enterprise_id,author_user_id,work_log_id,client_entry_key,content,display_order)"
                    + " VALUES(?,?,?,?,?,?,?)",
                id(),
                a.enterpriseId(),
                a.id(),
                journal,
                key,
                content,
                order);
    }

    /** 创建时间为准：当天创建（含当天补交的往期日志）当天可改可删，跨天即只读。 */
    private void checkEditWindow(IdentityService.Actor a, Map<String, Object> row) {
        if (!createdToday(a, row))
            throw new BusinessException(
                    409,
                    "JOURNAL_EDIT_WINDOW_EXPIRED",
                    "Only journals created today can be edited or deleted.",
                    Map.of());
    }

    @Transactional
    public Map<String, Object> delete(IdentityService.Actor a, String id) {
        a.business();
        db.lock("app_user", a.enterpriseId(), a.id());
        var row = db.lock("work_log", a.enterpriseId(), id);
        scope.read(a, str(row, "author_user_id"));
        if (!a.id().equals(row.get("author_user_id"))) throw BusinessException.forbidden();
        checkEditWindow(a, row);
        var auditBefore=audit.journal(a,id);
        for (var entry : db.rows(
                "SELECT id FROM work_log_entry WHERE enterprise_id=? AND work_log_id=?",
                a.enterpriseId(), id)) {
            removeSelectionReference(a, str(entry, "id"));
        }
        db.update(
                "DELETE FROM work_log_entry WHERE enterprise_id=? AND work_log_id=?",
                a.enterpriseId(),
                id);
        db.update(
                "DELETE FROM work_log_tag WHERE enterprise_id=? AND work_log_id=?",
                a.enterpriseId(),
                id);
        db.update("DELETE FROM work_log WHERE enterprise_id=? AND id=?", a.enterpriseId(), id);
        audit.record(a,id,"WORK_LOG","WORK_LOG","DELETE","删除工作日报",auditBefore,null);
        return Map.of("journalId", id);
    }

    private void removeSelectionReference(IdentityService.Actor a, String entryId) {
        int removed = db.update(
                "DELETE FROM personal_important_selection WHERE enterprise_id=? AND owner_user_id=? AND entry_id=?",
                a.enterpriseId(), a.id(), entryId);
        if (removed > 0) db.update(
                "UPDATE personal_important_selection_state SET version=version+1 WHERE enterprise_id=? AND owner_user_id=?",
                a.enterpriseId(), a.id());
    }

    @Transactional
    public Map<String, Object> patch(IdentityService.Actor a, String id, Contracts.JournalPatch c) {
        a.business();
        db.lock("app_user", a.enterpriseId(), a.id());
        var row = db.lock("work_log", a.enterpriseId(), id);
        scope.read(a, str(row, "author_user_id"));
        if (!a.id().equals(row.get("author_user_id"))) throw BusinessException.forbidden();
        checkEditWindow(a, row);
        var auditBefore=audit.journal(a,id);
        List<String> tagIds = null;
        if (c.tagIds() != null) tagIds = tags.validate(a, c.tagIds(), c.primaryTagId());
        else if (c.primaryTagId() != null)
            throw BusinessException.invalid("primaryTagId changes require the full tagIds set.");
        checkVersion(
                db.update(
                        "UPDATE work_log SET primary_tag_id=?,version=version+1 WHERE"
                                + " enterprise_id=? AND id=? AND version=?",
                        tagIds == null ? row.get("primary_tag_id") : c.primaryTagId(),
                        a.enterpriseId(),
                        id,
                        c.expectedVersion()));
        Set<String> touched = new HashSet<>(), newKeys = new HashSet<>();
        var old =
                db.rows(
                        "SELECT * FROM work_log_entry WHERE enterprise_id=? AND work_log_id=? ORDER"
                                + " BY display_order,id",
                        a.enterpriseId(),
                        id);
        int order = old.size();
        for (var entry :
                c.upsertEntries() == null ? List.<Contracts.EntryUpsert>of() : c.upsertEntries()) {
            if (entry.entryId() != null) {
                var existing =
                        db.one(
                                "SELECT * FROM work_log_entry WHERE enterprise_id=? AND"
                                        + " work_log_id=? AND author_user_id=? AND id=?",
                                a.enterpriseId(),
                                id,
                                a.id(),
                                entry.entryId());
                if (!touched.add(entry.entryId()))
                    throw BusinessException.invalid(
                            "An entry cannot be updated twice in one request.");
                if (entry.clientEntryKey() != null
                        && !entry.clientEntryKey().equals(existing.get("client_entry_key")))
                    throw BusinessException.invalid("Existing clientEntryKey is immutable.");
                db.update(
                        "UPDATE work_log_entry SET content=?,version=version+1 WHERE"
                                + " enterprise_id=? AND id=?",
                        entry.content(),
                        a.enterpriseId(),
                        entry.entryId());
            } else {
                if (entry.clientEntryKey() == null
                        || entry.clientEntryKey().isBlank()
                        || !newKeys.add(entry.clientEntryKey()))
                    throw BusinessException.invalid(
                            "New entries require unique clientEntryKey values.");
                insertEntry(a, id, entry.clientEntryKey(), entry.content(), order++);
            }
        }
        for (var entryId : c.deleteEntryIds() == null ? List.<String>of() : c.deleteEntryIds()) {
            if (!touched.add(entryId))
                throw BusinessException.invalid(
                        "An entry cannot be updated and deleted in one request.");
            removeSelectionReference(a, entryId);
            if (db.update(
                        "DELETE FROM work_log_entry WHERE enterprise_id=? AND work_log_id=? AND"
                            + " author_user_id=? AND id=?",
                        a.enterpriseId(),
                        id,
                        a.id(),
                        entryId)
                    != 1)
                throw BusinessException.invalid("Deleted entry does not belong to this journal.");
        }
        if (tagIds != null) tags.replace(a, "work_log_tag", "work_log_id", id, tagIds);
        audit.record(a,id,"WORK_LOG","WORK_LOG","UPDATE","修改工作日报",auditBefore,audit.journal(a,id));
        return detail(a, id);
    }

    private record CandidateQuery(String sql, List<Object> args) {}

    private CandidateQuery candidateQuery(IdentityService.Actor a) {
        a.business();
        LocalDate today = identity.today(a);
        return new CandidateQuery(
                "SELECT e.*,j.work_date FROM work_log_entry e JOIN work_log j ON"
                    + " j.enterprise_id=e.enterprise_id AND j.id=e.work_log_id WHERE"
                    + " e.enterprise_id=? AND e.author_user_id=? AND j.work_date BETWEEN ? AND ?",
                List.of(a.enterpriseId(), a.id(), today.minusDays(9), today));
    }

    private long candidateCount(CandidateQuery q) {
        return number(
                db.one("SELECT COUNT(*) AS total FROM (" + q.sql() + ") q", q.args().toArray()),
                "total");
    }

    public Map<String, Object> candidates(IdentityService.Actor a, String cursor, int limit) {
        var query = candidateQuery(a);
        return Pages.sqlPage(
                db,
                query.sql(),
                query.args(),
                cursor,
                limit,
                a.enterpriseId() + ":" + a.id() + ":entry-candidates:" + identity.today(a),
                List.of("work_date", "recorded_at", "id"),
                true,
                this::entryView);
    }

    public Map<String, Object> selection(IdentityService.Actor a) {
        a.business();
        var query = candidateQuery(a);
        long count = candidateCount(query);
        LocalDate today = identity.today(a);
        var state =
                db.optional(
                        "SELECT * FROM personal_important_selection_state WHERE enterprise_id=? AND"
                                + " owner_user_id=?",
                        a.enterpriseId(),
                        a.id());
        if (state.isEmpty())
            return fields(
                    "entries",
                    count <= 10 ? candidates(a, null, 10).get("items") : List.of(),
                    "hasManualSelection",
                    false,
                    "requiresSelection",
                    count > 10,
                    "candidateCount",
                    count,
                    "version",
                    0);
        var selected =
                db
                        .rows(
                                "SELECT e.*,j.work_date FROM personal_important_selection s JOIN"
                                    + " work_log_entry e ON e.enterprise_id=s.enterprise_id AND"
                                    + " e.author_user_id=s.owner_user_id AND e.id=s.entry_id JOIN"
                                    + " work_log j ON j.enterprise_id=e.enterprise_id AND"
                                    + " j.id=e.work_log_id WHERE s.enterprise_id=? AND"
                                    + " s.owner_user_id=? ORDER BY s.display_order,e.id",
                                a.enterpriseId(),
                                a.id())
                        .stream()
                        .map(
                                r -> {
                                    var v = entryView(r);
                                    LocalDate day =
                                            ((java.sql.Date) r.get("work_date")).toLocalDate();
                                    v.put(
                                            "eligible",
                                            !day.isBefore(today.minusDays(9))
                                                    && !day.isAfter(today));
                                    return v;
                                })
                        .toList();
        return fields(
                "entries",
                selected,
                "hasManualSelection",
                true,
                "requiresSelection",
                false,
                "candidateCount",
                count,
                "version",
                state.get().get("version"));
    }

    @Transactional
    public Map<String, Object> saveSelection(IdentityService.Actor a, Contracts.Selection c) {
        a.business();
        db.lock("app_user", a.enterpriseId(), a.id());
        List<String> ids = new ArrayList<>(new LinkedHashSet<>(c.entryIds()));
        if (ids.size() > 10)
            throw new BusinessException(
                    422, "TOO_MANY_IMPORTANT_ENTRIES", "Select at most ten entries.");
        var query = candidateQuery(a);
        Set<Object> eligible = new HashSet<>();
        if (!ids.isEmpty()) {
            var args = new ArrayList<>(query.args());
            args.addAll(ids);
            db.rows(
                            "SELECT id FROM ("
                                    + query.sql()
                                    + ") q WHERE id IN ("
                                    + Pages.placeholders(ids)
                                    + ")",
                            args.toArray())
                    .forEach(row -> eligible.add(row.get("id")));
        }
        Set<Object> previous = new HashSet<>();
        db.rows(
                        "SELECT entry_id FROM personal_important_selection WHERE enterprise_id=?"
                                + " AND owner_user_id=?",
                        a.enterpriseId(),
                        a.id())
                .forEach(r -> previous.add(r.get("entry_id")));
        if (ids.stream().anyMatch(id -> !eligible.contains(id) && !previous.contains(id)))
            throw new BusinessException(
                    422,
                    "ENTRY_NOT_ELIGIBLE",
                    "New selections must be your published entries within the last ten business"
                            + " dates.");
        var state =
                db.optional(
                        "SELECT * FROM personal_important_selection_state WHERE enterprise_id=? AND"
                                + " owner_user_id=?",
                        a.enterpriseId(),
                        a.id());
        if (state.isEmpty()) {
            if (c.expectedVersion() != 0) checkVersion(0);
            db.update(
                    "INSERT INTO"
                        + " personal_important_selection_state(enterprise_id,owner_user_id,version)"
                        + " VALUES(?,?,1)",
                    a.enterpriseId(),
                    a.id());
        } else
            checkVersion(
                    db.update(
                            "UPDATE personal_important_selection_state SET version=version+1 WHERE"
                                    + " enterprise_id=? AND owner_user_id=? AND version=?",
                            a.enterpriseId(),
                            a.id(),
                            c.expectedVersion()));
        db.update(
                "DELETE FROM personal_important_selection WHERE enterprise_id=? AND"
                        + " owner_user_id=?",
                a.enterpriseId(),
                a.id());
        for (int i = 0; i < ids.size(); i++)
            db.update(
                    "INSERT INTO personal_important_selection VALUES(?,?,?,?)",
                    a.enterpriseId(),
                    a.id(),
                    ids.get(i),
                    i);
        return selection(a);
    }
}
