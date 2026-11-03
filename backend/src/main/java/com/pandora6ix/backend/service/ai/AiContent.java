package com.pandora6ix.backend.service.ai;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 原创静态内容：塔罗牌库、人格题库、幸运签词表与兜底文案 */
@Component
public class AiContent {
    final JsonNode tarot;
    final JsonNode personality;
    final JsonNode fortune;

    public AiContent(JsonMapper json) {
        this.tarot = read(json, "tarot");
        this.personality = read(json, "personality");
        this.fortune = read(json, "fortune");
    }

    private static JsonNode read(JsonMapper json, String name) {
        try (var stream = new ClassPathResource("ai/content/" + name + ".json").getInputStream()) {
            return json.readTree(stream);
        } catch (IOException e) {
            throw new UncheckedIOException("Missing AI content " + name, e);
        }
    }
}
