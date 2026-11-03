package com.pandora6ix.backend.service.ai;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** 版本化提示词：共享规则 + 功能模板；用户数据放入 <input> 并声明为数据 */
@Component
public class AiPrompts {
    static final String VERSION = "v1";
    private final JsonMapper json;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public AiPrompts(JsonMapper json) {
        this.json = json;
    }

    String system(String feature) {
        return cache.computeIfAbsent(feature, key -> load("_rules") + "\n\n" + load(key));
    }

    String user(Object input) {
        return "<input>\n" + json.writeValueAsString(input) + "\n</input>";
    }

    private static String load(String name) {
        try (var stream = new ClassPathResource("ai/prompts/" + name + "." + VERSION + ".md").getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new UncheckedIOException("Missing AI prompt " + name, e);
        }
    }
}
