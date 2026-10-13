package com.pandora6ix.backend.service;

import static com.pandora6ix.backend.repository.BusinessDb.fields;

import com.pandora6ix.backend.api.BusinessException;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Cursors contain an opaque last stable row ID plus a fingerprint of the authorized query. */
public final class Pages {
    private Pages() {}

    public static String placeholders(Collection<?> values) {
        return String.join(",", Collections.nCopies(values.size(), "?"));
    }

    public static String like(String value) {
        return "%" + value.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    /** Fetch only one page. Stable non-null sort columns plus id support an opaque seek cursor. */
    public static Map<String, Object> sqlPage(
            com.pandora6ix.backend.repository.BusinessDb db,
            String query,
            List<Object> args,
            String cursor,
            int limit,
            String scope,
            List<String> sort,
            boolean descending,
            java.util.function.Function<Map<String, Object>, Map<String, Object>> projection) {
        if (limit < 1 || limit > 100)
            throw BusinessException.invalid("limit must be between 1 and 100.");
        String filter = "";
        List<Object> parameters = new ArrayList<>(args);
        if (cursor != null && !cursor.isBlank()) {
            String lastId;
            try {
                var parts =
                        new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)
                                .split("\\|", 2);
                if (parts.length != 2 || !parts[0].equals(IdentityService.hash(scope)))
                    throw new IllegalArgumentException();
                lastId = parts[1];
            } catch (IllegalArgumentException ex) {
                throw BusinessException.invalid(
                        "Cursor is invalid or its query changed; reload the list.");
            }
            var anchorArgs = new ArrayList<>(args);
            anchorArgs.add(lastId);
            var anchor =
                    db.optional("SELECT * FROM (" + query + ") q WHERE id=?", anchorArgs.toArray());
            if (anchor.isEmpty())
                throw BusinessException.invalid("Cursor anchor is unavailable; reload the list.");
            List<String> alternatives = new ArrayList<>();
            for (int i = 0; i < sort.size(); i++) {
                List<String> comparisons = new ArrayList<>();
                for (int j = 0; j < i; j++) {
                    comparisons.add(sort.get(j) + "=?");
                    parameters.add(anchor.get().get(sort.get(j)));
                }
                comparisons.add(sort.get(i) + (descending ? "<?" : ">?"));
                parameters.add(anchor.get().get(sort.get(i)));
                alternatives.add("(" + String.join(" AND ", comparisons) + ")");
            }
            filter = " WHERE " + String.join(" OR ", alternatives);
        }
        String order =
                sort.stream()
                        .map(s -> s + (descending ? " DESC" : " ASC"))
                        .collect(java.util.stream.Collectors.joining(","));
        parameters.add(limit + 1);
        var rows =
                db.rows(
                        "SELECT * FROM ("
                                + query
                                + ") q"
                                + filter
                                + " ORDER BY "
                                + order
                                + " LIMIT ?",
                        parameters.toArray());
        boolean more = rows.size() > limit;
        var visible = more ? rows.subList(0, limit) : rows;
        String next =
                more
                        ? Base64.getUrlEncoder()
                                .withoutPadding()
                                .encodeToString(
                                        (IdentityService.hash(scope)
                                                        + "|"
                                                        + visible.getLast().get("id"))
                                                .getBytes(StandardCharsets.UTF_8))
                        : null;
        return fields("items", visible.stream().map(projection).toList(), "nextCursor", next);
    }
}
