package com.pandora6ix.backend.service;

import static com.pandora6ix.backend.api.BusinessException.checkVersion;
import static com.pandora6ix.backend.repository.BusinessDb.*;

import com.pandora6ix.backend.api.*;
import com.pandora6ix.backend.repository.BusinessDb;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class JournalService implements com.pandora6ix.backend.application.journals.JournalUseCases {
    private final BusinessDb db;
    private final IdentityService identity;
    private final ScopeService scope;
    private final TagService tags;

    public JournalService(
            BusinessDb db, IdentityService identity, ScopeService scope, TagService tags) {
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
        return result;
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
        identity.zone(a);
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

    /** 24 小时编辑窗口：自创建时刻（published_at）起算，超窗禁止修改或删除 */
    private void checkEditWindow(Map<String, Object> row) {
        var published = (OffsetDateTime) row.get("published_at");
        if (published != null
                && published.toInstant().isBefore(Instant.now().minus(Duration.ofHours(24))))
            throw new BusinessException(
                    409,
                    "JOURNAL_EDIT_WINDOW_EXPIRED",
                    "Journals can only be edited or deleted within 24 hours after creation.",
                    Map.of());
    }

    @Transactional
    public Map<String, Object> delete(IdentityService.Actor a, String id) {
        a.business();
        var row = db.lock("work_log", a.enterpriseId(), id);
        scope.read(a, str(row, "author_user_id"));
        if (!a.id().equals(row.get("author_user_id"))) throw BusinessException.forbidden();
        checkEditWindow(row);
        db.update(
                "DELETE FROM work_log_entry WHERE enterprise_id=? AND work_log_id=?",
                a.enterpriseId(),
                id);
        db.update(
                "DELETE FROM work_log_tag WHERE enterprise_id=? AND work_log_id=?",
                a.enterpriseId(),
                id);
        db.update("DELETE FROM work_log WHERE enterprise_id=? AND id=?", a.enterpriseId(), id);
        return Map.of("journalId", id);
    }

    @Transactional
    public Map<String, Object> patch(IdentityService.Actor a, String id, Contracts.JournalPatch c) {
        a.business();
        var row = db.lock("work_log", a.enterpriseId(), id);
        scope.read(a, str(row, "author_user_id"));
        if (!a.id().equals(row.get("author_user_id"))) throw BusinessException.forbidden();
        checkEditWindow(row);
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
