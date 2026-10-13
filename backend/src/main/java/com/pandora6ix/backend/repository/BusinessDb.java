package com.pandora6ix.backend.repository;

import com.pandora6ix.backend.api.BusinessException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.*;

@Repository
public class BusinessDb {
    private final JdbcTemplate jdbc;

    public BusinessDb(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int update(String sql, Object... args) {
        return jdbc.update(sql, args);
    }

    public List<Map<String, Object>> rows(String sql, Object... args) {
        return jdbc.query(
                sql,
                (rs, n) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        Object v = rs.getObject(i);
                        if (v instanceof Clob c) v = c.getSubString(1, (int) c.length());
                        row.put(rs.getMetaData().getColumnLabel(i).toLowerCase(Locale.ROOT), v);
                    }
                    return row;
                },
                args);
    }

    public Map<String, Object> one(String sql, Object... args) {
        var r = rows(sql, args);
        if (r.isEmpty()) throw BusinessException.missing();
        return r.getFirst();
    }

    public Optional<Map<String, Object>> optional(String sql, Object... args) {
        return rows(sql, args).stream().findFirst();
    }

    public Map<String, Object> entity(String table, String enterprise, String id) {
        return one("SELECT * FROM " + table + " WHERE enterprise_id=? AND id=?", enterprise, id);
    }

    public Map<String, Object> lock(String table, String enterprise, String id) {
        return one(
                "SELECT * FROM " + table + " WHERE enterprise_id=? AND id=? FOR UPDATE",
                enterprise,
                id);
    }

    public static String id() {
        return UUID.randomUUID().toString();
    }

    public static String str(Map<String, Object> row, String key) {
        var v = row.get(key);
        return v == null ? null : v.toString();
    }

    public static long number(Map<String, Object> row, String key) {
        return ((Number) row.get(key)).longValue();
    }

    public static Map<String, Object> fields(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }

    public static Map<String, Object> view(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        row.forEach(
                (k, v) -> {
                    if (Set.of("password_hash", "token_hash", "enterprise_id", "submitted_snapshot")
                            .contains(k)) return;
                    StringBuilder camel = new StringBuilder();
                    boolean upper = false;
                    for (char c : k.toCharArray()) {
                        if (c == '_') upper = true;
                        else {
                            camel.append(upper ? Character.toUpperCase(c) : c);
                            upper = false;
                        }
                    }
                    if (v instanceof java.sql.Date d) v = d.toLocalDate().toString();
                    else if (v instanceof java.time.OffsetDateTime d) v = d.toInstant().toString();
                    else if (v instanceof java.sql.Timestamp d) v = d.toInstant().toString();
                    result.put(camel.toString(), v);
                });
        return result;
    }
}
