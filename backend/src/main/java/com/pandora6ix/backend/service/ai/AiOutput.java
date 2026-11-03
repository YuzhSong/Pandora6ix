package com.pandora6ix.backend.service.ai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;

/** 模型输出校验：必填字段、数组数量、按字数截断；不合格即视为本次生成失败 */
final class AiOutput {
    private AiOutput() {}

    static final class InvalidOutputException extends IOException {
        InvalidOutputException(String message) { super(message); }
    }

    static String text(JsonNode node, String field, int max) throws InvalidOutputException {
        JsonNode value = node.path(field);
        String text = value.isString() ? value.asString().trim() : "";
        if (text.isEmpty()) throw new InvalidOutputException("missing " + field);
        return truncate(text, max);
    }

    /** 数组至少 min 项；超过 max 只取前 max 项 */
    static List<JsonNode> array(JsonNode node, String field, int min, int max) throws InvalidOutputException {
        JsonNode value = node.path(field);
        if (!value.isArray() || value.size() < min) throw new InvalidOutputException("bad array " + field);
        List<JsonNode> items = new ArrayList<>();
        for (int i = 0; i < Math.min(max, value.size()); i++) items.add(value.get(i));
        return items;
    }

    static List<String> texts(JsonNode node, String field, int min, int max, int maxLength) throws InvalidOutputException {
        List<String> result = new ArrayList<>();
        for (JsonNode item : array(node, field, min, Integer.MAX_VALUE)) {
            String text = item.isString() ? item.asString().trim() : "";
            if (!text.isEmpty() && result.size() < max) result.add(truncate(text, maxLength));
        }
        if (result.size() < min) throw new InvalidOutputException("bad items " + field);
        return result;
    }

    static String truncate(String text, int max) {
        String single = text.replaceAll("\\s+", " ").trim();
        if (single.codePointCount(0, single.length()) <= max) return single;
        return single.substring(0, single.offsetByCodePoints(0, max - 1)) + "…";
    }
}
