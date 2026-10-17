package com.pandora6ix.backend.service;

import static com.pandora6ix.backend.repository.BusinessDb.*;

import com.pandora6ix.backend.api.*;
import com.pandora6ix.backend.repository.BusinessDb;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TagService implements com.pandora6ix.backend.application.tags.TagUseCases {
    private final BusinessDb db;

    public TagService(BusinessDb db) {
        this.db = db;
    }

    public Map<String, Object> list(
            IdentityService.Actor a, String name, String cursor, int limit) {
        String query = name == null ? "" : name.toLowerCase(Locale.ROOT);
        return Pages.sqlPage(
                db,
                "SELECT * FROM enterprise_tag WHERE enterprise_id=? AND LOWER(name) LIKE ? ESCAPE"
                        + " '!'",
                List.of(a.enterpriseId(), Pages.like(query)),
                cursor,
                limit,
                a.enterpriseId() + ":tags:" + query,
                List.of("name", "id"),
                false,
                BusinessDb::view);
    }

    @Transactional
    public Map<String, Object> create(IdentityService.Actor a, Contracts.TagCreate c) {
        if (!a.has("ADMIN") && !a.has("FOUNDER") && !a.has("DEPARTMENT_MANAGER"))
            throw BusinessException.forbidden();
        String id = id();
        db.update(
                "INSERT INTO enterprise_tag(id,enterprise_id,name,color_hex,creator_user_id)"
                        + " VALUES(?,?,?,?,?)",
                id,
                a.enterpriseId(),
                c.name(),
                c.colorHex(),
                a.id());
        return view(db.entity("enterprise_tag", a.enterpriseId(), id));
    }

    @Transactional
    public Map<String, Object> patch(IdentityService.Actor a, String id, Contracts.TagPatch c) {
        a.admin();
        db.entity("enterprise_tag", a.enterpriseId(), id);
        if (c.name() != null && c.name().isBlank())
            throw BusinessException.invalid("Tag name must not be blank.");
        BusinessException.checkVersion(
                db.update(
                        "UPDATE enterprise_tag SET"
                            + " name=COALESCE(?,name),color_hex=COALESCE(?,color_hex),version=version+1"
                            + " WHERE enterprise_id=? AND id=? AND version=?",
                        c.name(),
                        c.colorHex(),
                        a.enterpriseId(),
                        id,
                        c.expectedVersion()));
        return view(db.entity("enterprise_tag", a.enterpriseId(), id));
    }

    /**
     * 删除标签：与 create 同权限范围（ADMIN/FOUNDER/DEPARTMENT_MANAGER，未来按等级细分）。
     * 级联清理引用：任务/日报的标签关联行，任务与日报的主标签置空（快照为 JSON 不受影响）。
     */
    @Transactional
    public Map<String, Object> delete(IdentityService.Actor a, String id) {
        if (!a.has("ADMIN") && !a.has("FOUNDER") && !a.has("DEPARTMENT_MANAGER"))
            throw BusinessException.forbidden();
        db.entity("enterprise_tag", a.enterpriseId(), id);
        db.update("DELETE FROM task_tag WHERE enterprise_id=? AND tag_id=?", a.enterpriseId(), id);
        db.update("DELETE FROM work_log_tag WHERE enterprise_id=? AND tag_id=?", a.enterpriseId(), id);
        db.update("UPDATE task SET primary_tag_id=NULL WHERE enterprise_id=? AND primary_tag_id=?", a.enterpriseId(), id);
        db.update("UPDATE work_log SET primary_tag_id=NULL WHERE enterprise_id=? AND primary_tag_id=?", a.enterpriseId(), id);
        db.update("DELETE FROM enterprise_tag WHERE enterprise_id=? AND id=?", a.enterpriseId(), id);
        return Map.of("tagId", id);
    }

    public List<String> validate(IdentityService.Actor a, List<String> tags, String primary) {
        var ids = tags == null ? List.<String>of() : new ArrayList<>(new LinkedHashSet<>(tags));
        if (ids.isEmpty() ? primary != null : primary == null || !ids.contains(primary))
            throw new BusinessException(
                    422,
                    "TAG_SET_INVALID",
                    "A nonempty tag set requires one primary tag in the set.");
        for (String id : ids)
            if (db.optional(
                            "SELECT id FROM enterprise_tag WHERE enterprise_id=? AND id=?",
                            a.enterpriseId(),
                            id)
                    .isEmpty())
                throw new BusinessException(
                        422, "TAG_SET_INVALID", "Tags must belong to the current enterprise.");
        return ids;
    }

    public void replace(
            IdentityService.Actor a,
            String table,
            String objectColumn,
            String id,
            List<String> tags) {
        db.update(
                "DELETE FROM " + table + " WHERE enterprise_id=? AND " + objectColumn + "=?",
                a.enterpriseId(),
                id);
        for (String tag : tags)
            db.update(
                    "INSERT INTO "
                            + table
                            + "(enterprise_id,"
                            + objectColumn
                            + ",tag_id) VALUES(?,?,?)",
                    a.enterpriseId(),
                    id,
                    tag);
    }

    public List<Map<String, Object>> linked(
            IdentityService.Actor a, String table, String objectColumn, String id) {
        return db
                .rows(
                        "SELECT t.* FROM enterprise_tag t JOIN "
                                + table
                                + " link ON link.enterprise_id=t.enterprise_id AND link.tag_id=t.id"
                                + " WHERE t.enterprise_id=? AND link."
                                + objectColumn
                                + "=? ORDER BY t.id",
                        a.enterpriseId(),
                        id)
                .stream()
                .map(BusinessDb::view)
                .toList();
    }
}
