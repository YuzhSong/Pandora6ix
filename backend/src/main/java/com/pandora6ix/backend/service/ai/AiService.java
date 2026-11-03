package com.pandora6ix.backend.service.ai;

import static com.pandora6ix.backend.repository.BusinessDb.fields;

import com.pandora6ix.backend.api.BusinessException;
import com.pandora6ix.backend.api.Contracts;
import com.pandora6ix.backend.service.IdentityService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

/**
 * AI 探索馆五项功能。程序决定确定性结果（牌、人格、星级等），模型只写解读；
 * 任何失败都返回兜底内容，不写入任何业务数据。
 */
@Service
public class AiService {
    static final String DISCLAIMER = "以上内容由 AI 生成，仅供娱乐与参考，不构成任何评价、预测或建议。";
    static final String FALLBACK_DISCLAIMER = "以上为基础版内容，仅供娱乐与参考，不构成任何评价、预测或建议。";
    private static final List<String> DIMENSIONS = List.of("plan", "collab", "innovate", "focus");
    private final IdentityService identity;
    private final AiEngine engine;
    private final AiContent content;
    private final AiDigestBuilder digests;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Map<String, Object>> fortunes = new ConcurrentHashMap<>();

    public AiService(IdentityService identity, AiEngine engine, AiContent content, AiDigestBuilder digests) {
        this.identity = identity;
        this.engine = engine;
        this.content = content;
        this.digests = digests;
    }

    // ---------- 今日幸运签 ----------

    public Map<String, Object> fortune(IdentityService.Actor a, String zodiac) {
        a.business();
        String sign = zodiac == null || zodiac.isBlank() ? null : zodiac.trim().toUpperCase(Locale.ROOT);
        if (sign != null && !content.fortune.path("zodiacs").has(sign)) throw BusinessException.invalid("Unknown zodiac sign.");
        LocalDate today = identity.today(a);
        String key = a.enterpriseId() + "|" + a.id() + "|" + today + "|" + sign;
        var cached = fortunes.get(key);
        if (cached != null) return cached;

        byte[] seed = sha256(a.enterpriseId() + "|" + a.id() + "|" + today + "|" + sign);
        JsonNode table = content.fortune;
        JsonNode color = table.path("colors").get(pick(seed, 1, table.path("colors").size()));
        var data = new LinkedHashMap<String, Object>();
        data.put("date", today.toString());
        data.put("zodiac", sign);
        data.put("zodiacName", sign == null ? null : table.path("zodiacs").path(sign).asString());
        data.put("stars", 2 + pick(seed, 0, 4));
        data.put("luckyColor", fields("name", color.path("name").asString(), "hex", color.path("hex").asString()));
        data.put("keyword", table.path("keywords").get(pick(seed, 2, table.path("keywords").size())).asString());
        data.put("luckyAction", table.path("actions").get(pick(seed, 3, table.path("actions").size())).asString());
        data.put("workStars", 2 + pick(seed, 4, 4));
        data.put("socialStars", 2 + pick(seed, 5, 4));
        data.put("inspirationStars", 2 + pick(seed, 6, 4));
        data.put("slackIndex", pick(seed, 7, 101));

        var generation = engine.generate(a.id(), today, "fortune", data, 1.2, 400, output -> fields(
                "message", AiOutput.text(output, "message", 60),
                "workTip", AiOutput.text(output, "workTip", 40),
                "socialTip", AiOutput.text(output, "socialTip", 40),
                "inspirationTip", AiOutput.text(output, "inspirationTip", 40)));
        data.putAll(generation.generated() ? generation.fields() : fields(
                "message", table.path("messages").get(pick(seed, 8, table.path("messages").size())).asString(),
                "workTip", "先完成最重要的一件事，其余的都会变轻松。",
                "socialTip", "主动和身边的人打个招呼，好心情会传染。",
                "inspirationTip", "换个地方坐一会儿，灵感可能就在转角。"));
        var result = result("fortune", generation, data);
        // 同一业务日只生成一次；兜底结果不缓存，便于服务恢复后得到 AI 寄语
        if (generation.generated()) {
            if (fortunes.size() > 10_000) fortunes.keySet().removeIf(k -> !k.contains("|" + today + "|"));
            fortunes.put(key, result);
        }
        return result;
    }

    // ---------- 职场人格盲盒 ----------

    public Map<String, Object> personalityQuestions(IdentityService.Actor a) {
        a.business();
        JsonNode bank = content.personality;
        List<Map<String, Object>> questions = new ArrayList<>();
        for (JsonNode q : bank.path("questions")) {
            List<Map<String, Object>> options = new ArrayList<>();
            for (JsonNode o : q.path("options")) options.add(fields("id", q.path("id").asString() + "-" + o.path("id").asString(), "text", o.path("text").asString()));
            questions.add(fields("id", q.path("id").asString(), "text", q.path("text").asString(), "options", options));
        }
        return fields("version", bank.path("version").asString(), "questions", questions);
    }

    public Map<String, Object> personality(IdentityService.Actor a, Contracts.PersonalityAnswers c) {
        a.business();
        JsonNode bank = content.personality;
        Map<String, JsonNode> chosen = new LinkedHashMap<>();
        Map<String, String> questionText = new LinkedHashMap<>();
        for (JsonNode q : bank.path("questions")) questionText.put(q.path("id").asString(), q.path("text").asString());
        for (String optionId : c.optionIds()) {
            int dash = optionId.indexOf('-');
            String questionId = dash < 0 ? "" : optionId.substring(0, dash), option = dash < 0 ? "" : optionId.substring(dash + 1);
            JsonNode match = null;
            for (JsonNode q : bank.path("questions"))
                if (q.path("id").asString().equals(questionId))
                    for (JsonNode o : q.path("options")) if (o.path("id").asString().equals(option)) match = o;
            if (match == null || chosen.put(questionId, match) != null)
                throw BusinessException.invalid("Each question must be answered exactly once with a known option.");
        }
        if (chosen.size() != questionText.size()) throw BusinessException.invalid("Each question must be answered exactly once with a known option.");

        Map<String, Integer> points = new LinkedHashMap<>(), maxima = new LinkedHashMap<>();
        for (String d : DIMENSIONS) { points.put(d, 0); maxima.put(d, 0); }
        for (JsonNode q : bank.path("questions"))
            for (String d : DIMENSIONS) {
                int best = 0;
                for (JsonNode o : q.path("options")) best = Math.max(best, o.path("scores").path(d).asInt(0));
                maxima.merge(d, best, Integer::sum);
            }
        chosen.values().forEach(o -> DIMENSIONS.forEach(d -> points.merge(d, o.path("scores").path(d).asInt(0), Integer::sum)));
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (String d : DIMENSIONS) scores.put(d, maxima.get(d) == 0 ? 0 : Math.round(points.get(d) * 100f / maxima.get(d)));
        // 维度并列时按固定顺序（规划、协作、创新、专注）取先者，保证同样答案得到同样人格
        List<String> ranked = new ArrayList<>(DIMENSIONS);
        ranked.sort((x, y) -> Integer.compare(scores.get(y), scores.get(x)));
        String archetypeKey = bank.path("pairing").path(ranked.get(0) + ">" + ranked.get(1)).asString();
        JsonNode archetype = bank.path("archetypes").path(archetypeKey);

        List<Map<String, Object>> dimensions = new ArrayList<>();
        for (JsonNode d : bank.path("dimensions")) dimensions.add(fields("key", d.path("key").asString(), "name", d.path("name").asString(), "score", scores.get(d.path("key").asString())));
        var data = new LinkedHashMap<String, Object>();
        data.put("archetype", fields("key", archetypeKey, "name", archetype.path("name").asString(), "code", archetype.path("code").asString(),
                "emoji", archetype.path("emoji").asString(), "tagline", archetype.path("tagline").asString()));
        data.put("dimensions", dimensions);
        data.put("slackTalent", Math.max(1, Math.min(5, 5 - scores.get("focus") / 25)));

        List<Map<String, Object>> highlights = new ArrayList<>();
        chosen.forEach((q, o) -> highlights.add(fields("question", questionText.get(q), "answer", o.path("text").asString())));
        var input = fields("archetype", fields("name", archetype.path("name").asString(), "tagline", archetype.path("tagline").asString()),
                "dimensions", dimensions, "highlights", highlights);
        LocalDate today = identity.today(a);
        var generation = engine.generate(a.id(), today, "personality", input, 1.0, 700, output -> fields(
                "headline", AiOutput.text(output, "headline", 30),
                "portrait", AiOutput.text(output, "portrait", 160),
                "strengths", AiOutput.texts(output, "strengths", 3, 3, 30),
                "watchouts", AiOutput.texts(output, "watchouts", 2, 2, 30),
                "workStyleTip", AiOutput.text(output, "workStyleTip", 60)));
        data.putAll(generation.generated() ? generation.fields() : fields(
                "headline", "你是" + archetype.path("name").asString() + "：" + archetype.path("tagline").asString(),
                "portrait", archetype.path("portrait").asString(),
                "strengths", strings(archetype.path("strengths")),
                "watchouts", strings(archetype.path("watchouts")),
                "workStyleTip", archetype.path("workStyleTip").asString()));
        return result("personality", generation, data);
    }

    // ---------- AI 塔罗屋 ----------

    public Map<String, Object> tarot(IdentityService.Actor a, Contracts.TarotDraw c) {
        a.business();
        String question = c.question() == null ? "" : new AiRedactor(List.of()).excerpt(c.question(), 100);
        JsonNode deck = content.tarot.path("cards");
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < deck.size(); i++) order.add(i);
        Collections.shuffle(order, random);
        List<Map<String, Object>> cards = new ArrayList<>();
        JsonNode positions = content.tarot.path("positions");
        for (int i = 0; i < 3; i++) {
            JsonNode card = deck.get(order.get(i));
            boolean upright = random.nextBoolean();
            var view = new LinkedHashMap<String, Object>();
            view.put("position", positions.get(i).asString());
            view.put("number", card.path("number").asInt());
            view.put("name", card.path("name").asString());
            view.put("upright", upright);
            view.put("keywords", strings(card.path(upright ? "upright" : "reversed")));
            cards.add(view);
        }
        var input = fields("question", question, "cards", cards);
        LocalDate today = identity.today(a);
        var generation = engine.generate(a.id(), today, "tarot", input, 1.2, 900, output -> {
            var readings = AiOutput.array(output, "cards", 3, 3);
            List<String> texts = new ArrayList<>();
            for (JsonNode reading : readings) texts.add(AiOutput.text(reading, "reading", 120));
            return fields("readings", texts, "summary", AiOutput.text(output, "summary", 120), "advice", AiOutput.text(output, "advice", 60));
        });
        List<String> readings = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            JsonNode card = deck.get(order.get(i));
            readings.add(generation.generated()
                    ? ((List<?>) generation.fields().get("readings")).get(i).toString()
                    : card.path(Boolean.TRUE.equals(cards.get(i).get("upright")) ? "uprightMeaning" : "reversedMeaning").asString());
            cards.get(i).put("reading", readings.get(i));
        }
        var data = new LinkedHashMap<String, Object>();
        data.put("question", question.isBlank() ? null : question);
        data.put("cards", cards);
        data.put("summary", generation.generated() ? generation.fields().get("summary")
                : "从" + cards.get(0).get("name") + "到" + cards.get(2).get("name") + "，你正在经历一段自然的变化，保持好奇，慢慢来。");
        data.put("advice", generation.generated() ? generation.fields().get("advice") : "今天挑一件小事认真做完，给自己一点确定感。");
        return result("tarot", generation, data);
    }

    // ---------- 我的职业星图 ----------

    public Map<String, Object> careerMap(IdentityService.Actor a) {
        a.business();
        LocalDate today = identity.today(a);
        var digest = digests.career(a, today);
        var data = new LinkedHashMap<String, Object>();
        data.put("empty", digest.empty());
        data.put("stats", digest.stats());
        data.put("topTags", digest.topTags());
        if (digest.empty()) {
            data.put("guidance", "近 30 天还没有日志或任务记录。坚持记录一段时间后，再来生成你的职业星图吧！");
            return result("career-map", new AiEngine.Generation(null, false), data);
        }
        var generation = engine.generate(a.id(), today, "career-map", digest.input(), 0.7, 1200, output -> {
            List<Map<String, Object>> abilities = new ArrayList<>();
            for (JsonNode item : AiOutput.array(output, "abilities", 3, 6))
                abilities.add(fields("name", AiOutput.text(item, "name", 12), "level", level(item.path("level").asString()),
                        "evidence", AiOutput.text(item, "evidence", 40)));
            return fields("label", AiOutput.text(output, "label", 12), "summary", AiOutput.text(output, "summary", 120),
                    "abilities", abilities, "routes", routes(output));
        });
        data.putAll(generation.generated() ? generation.fields() : careerFallback(digest));
        return result("career-map", generation, data);
    }

    private static List<Map<String, Object>> routes(JsonNode output) throws AiOutput.InvalidOutputException {
        List<Map<String, Object>> routes = new ArrayList<>();
        String[] keys = {"A", "B", "C"};
        var items = AiOutput.array(output, "routes", 3, 3);
        for (int i = 0; i < 3; i++)
            routes.add(fields("key", keys[i], "title", AiOutput.text(items.get(i), "title", 12),
                    "description", AiOutput.text(items.get(i), "description", 80),
                    "actions", AiOutput.texts(items.get(i), "actions", 1, 3, 30)));
        return routes;
    }

    private static String level(String value) throws AiOutput.InvalidOutputException {
        String level = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!List.of("HIGH", "MEDIUM", "LOW", "INSUFFICIENT").contains(level)) throw new AiOutput.InvalidOutputException("bad level");
        return level;
    }

    private static Map<String, Object> careerFallback(AiDigestBuilder.Digest digest) {
        List<Map<String, Object>> abilities = new ArrayList<>();
        for (var tag : digest.topTags().subList(0, Math.min(4, digest.topTags().size()))) {
            long count = (long) tag.get("count");
            abilities.add(fields("name", tag.get("name"), "level", count >= 8 ? "HIGH" : count >= 3 ? "MEDIUM" : "LOW",
                    "evidence", "近 30 天日志标签出现 " + count + " 次"));
        }
        if (abilities.isEmpty()) abilities.add(fields("name", "工作记录", "level", "INSUFFICIENT", "evidence", "记录中暂无充分信息"));
        var stats = digest.stats();
        return fields(
                "label", "成长探索者",
                "summary", "近 30 天你写了 " + stats.get("journalCount") + " 篇日志、参与 " + stats.get("taskCount") + " 项任务，正在稳步积累自己的工作版图。",
                "abilities", abilities,
                "routes", List.of(
                        fields("key", "A", "title", "专业深耕", "description", "在当前最常做的方向上持续深入，逐步成为团队里的可靠专家。",
                                "actions", List.of("每周总结一个专业小技巧", "主动承担一个有挑战的任务", "整理一份常见问题清单")),
                        fields("key", "B", "title", "项目推进", "description", "多参与需求沟通与任务协调，练习把事情从开始推进到完成。",
                                "actions", List.of("尝试负责一次小型会议", "为任务拆分里程碑", "复盘一次项目经验")),
                        fields("key", "C", "title", "创新探索", "description", "尝试跨领域的新工作，把新工具和新方法带进团队。",
                                "actions", List.of("每月学习一个新工具", "提出一个改进建议", "和其他部门交流一次"))));
    }

    // ---------- 公司下一站 ----------

    public Map<String, Object> companyOutlook(IdentityService.Actor a) {
        a.business();
        LocalDate today = identity.today(a);
        var digest = digests.company(a, today);
        var data = new LinkedHashMap<String, Object>();
        data.put("empty", digest.empty());
        data.put("stats", digest.stats());
        data.put("topTags", digest.topTags());
        if (digest.empty()) {
            data.put("guidance", "目前还没有可见的公司重要事项或任务记录，等数据多一些再来看看公司的下一站吧！");
            return result("company-outlook", new AiEngine.Generation(null, false), data);
        }
        var generation = engine.generate(a.id(), today, "company-outlook", digest.input(), 0.7, 1200, output -> {
            List<Map<String, Object>> scenarios = new ArrayList<>();
            String[] keys = {"RAPID", "STEADY", "CHALLENGE"};
            var items = AiOutput.array(output, "scenarios", 3, 3);
            for (int i = 0; i < 3; i++)
                scenarios.add(fields("key", keys[i], "title", AiOutput.text(items.get(i), "title", 12),
                        "developments", AiOutput.text(items.get(i), "developments", 120),
                        "signals", AiOutput.texts(items.get(i), "signals", 1, 3, 40),
                        "preparations", AiOutput.texts(items.get(i), "preparations", 1, 3, 40)));
            return fields("overview", AiOutput.text(output, "overview", 120), "scenarios", scenarios);
        });
        data.putAll(generation.generated() ? generation.fields() : companyFallback(digest));
        return result("company-outlook", generation, data);
    }

    private static Map<String, Object> companyFallback(AiDigestBuilder.Digest digest) {
        var s = digest.stats();
        String volume = "近 90 天可见任务 " + s.get("taskCount") + " 项";
        String done = "已完成执行 " + s.get("completedCount") + " 项，进行中 " + s.get("inProgressCount") + " 项";
        String overdue = "逾期未完成 " + s.get("overdueCount") + " 项";
        String items = "公司重要事项 " + s.get("importantItemCount") + " 条";
        return fields(
                "overview", "根据你可见的 " + items + "和" + volume + "，公司正处在持续推进的阶段。下面是三种可能的发展剧本。",
                "scenarios", List.of(
                        fields("key", "RAPID", "title", "高速发展", "developments", "如果重点事项顺利推进，公司可能进入成果交付与业务拓展阶段。",
                                "signals", List.of(volume, done), "preparations", List.of("提前梳理交付流程", "为新业务预留人手")),
                        fields("key", "STEADY", "title", "稳健前进", "developments", "保持当前节奏，可以把重心放在提升内部效率和质量上。",
                                "signals", List.of(items, done), "preparations", List.of("定期复盘任务完成情况", "沉淀可复用的工作方法")),
                        fields("key", "CHALLENGE", "title", "挑战模式", "developments", "如果关键任务持续延期，可能需要重新调整资源与优先级。",
                                "signals", List.of(overdue), "preparations", List.of("识别影响进度的关键任务", "提前沟通调整计划"))));
    }

    // ---------- 公共 ----------

    private static Map<String, Object> result(String feature, AiEngine.Generation generation, Map<String, Object> data) {
        return fields("feature", feature, "aiGenerated", generation.generated(), "quotaExceeded", generation.quotaExceeded(),
                "disclaimer", generation.generated() ? DISCLAIMER : FALLBACK_DISCLAIMER,
                "generatedAt", Instant.now().toString(), "data", data);
    }

    private static List<String> strings(JsonNode array) {
        List<String> result = new ArrayList<>();
        for (JsonNode item : array) result.add(item.asString());
        return result;
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 用摘要中第 slot 组两个字节确定性地取 [0, bound) */
    private static int pick(byte[] seed, int slot, int bound) {
        int value = ((seed[slot * 2] & 0xff) << 8) | (seed[slot * 2 + 1] & 0xff);
        return value % bound;
    }
}
