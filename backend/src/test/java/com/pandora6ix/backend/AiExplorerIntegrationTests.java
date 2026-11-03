package com.pandora6ix.backend;

import static org.junit.jupiter.api.Assertions.*;

import com.pandora6ix.backend.repository.BusinessDb;
import com.pandora6ix.backend.service.IdentityService;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.json.JsonMapper;

/** AI 探索馆接口：以本地假 DeepSeek 服务验证鉴权、数据最小化、确定性结果与只读性 */
@SpringBootTest
@AutoConfigureMockMvc
class AiExplorerIntegrationTests {
    private static final List<String> REQUESTS = new CopyOnWriteArrayList<>();
    private static final HttpServer DEEPSEEK = fakeDeepSeek();

    @DynamicPropertySource
    static void deepSeek(DynamicPropertyRegistry registry) {
        registry.add("pandora.deepseek.api-key", () -> "fixture-key");
        registry.add("pandora.deepseek.base-url", () -> "http://127.0.0.1:" + DEEPSEEK.getAddress().getPort());
        registry.add("pandora.ai.daily-quota", () -> "100");
    }

    @AfterAll
    static void stop() {
        DEEPSEEK.stop(0);
    }

    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired BusinessDb db;
    private String enterprise, me, colleague, outsider, myToken, outsiderToken;

    /** 一个同时满足五个功能输出要求的 JSON，便于用同一假服务验证解析与校验 */
    private static HttpServer fakeDeepSeek() {
        try {
            var mapper = JsonMapper.builder().build();
            var reading = List.of(Map.of("position", "过去", "reading", "r1"), Map.of("position", "现在", "reading", "r2"), Map.of("position", "未来", "reading", "r3"));
            var route = Map.of("title", "路线", "description", "描述", "actions", List.of("一", "二", "三"));
            var scenario = Map.of("title", "情景", "developments", "发展", "signals", List.of("信号"), "preparations", List.of("准备一", "准备二"));
            var output = new LinkedHashMap<String, Object>();
            output.putAll(Map.of("message", "AI 寄语", "workTip", "工作", "socialTip", "社交", "inspirationTip", "灵感"));
            output.putAll(Map.of("headline", "AI 标题", "portrait", "画像", "strengths", List.of("甲", "乙", "丙"), "watchouts", List.of("丁", "戊"), "workStyleTip", "建议"));
            output.putAll(Map.of("cards", reading, "summary", "AI 总结", "advice", "行动"));
            output.putAll(Map.of("label", "全栈探索者", "abilities", List.of(Map.of("name", "开发", "level", "high", "evidence", "日志"),
                    Map.of("name", "协作", "level", "MEDIUM", "evidence", "日志"), Map.of("name", "测试", "level", "LOW", "evidence", "日志")),
                    "routes", List.of(route, route, route)));
            output.putAll(Map.of("overview", "AI 概览", "scenarios", List.of(scenario, scenario, scenario)));
            byte[] response = mapper.writeValueAsString(Map.of("choices", List.of(Map.of("message", Map.of("content", mapper.writeValueAsString(output))))))
                    .getBytes(StandardCharsets.UTF_8);
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/chat/completions", exchange -> {
                REQUESTS.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();
            return server;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @BeforeEach
    void seed() {
        REQUESTS.clear();
        enterprise = UUID.randomUUID().toString();
        db.update("INSERT INTO enterprise(id,login_domain,name,business_timezone) VALUES(?,?,?,?)", enterprise, "ai-" + enterprise, "AI 测试企业", "Asia/Shanghai");
        me = user("zhangsf", "张三丰");
        colleague = user("lixl", "李小龙");
        outsider = user("wangwl", "王五六");
        myToken = session(me);
        outsiderToken = session(outsider);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        String tag = BusinessDb.id(), log = BusinessDb.id();
        db.update("INSERT INTO enterprise_tag(id,enterprise_id,name,color_hex,creator_user_id) VALUES(?,?,?,?,?)", tag, enterprise, "Android开发", "#5B8DEF", me);
        db.update("INSERT INTO work_log(id,enterprise_id,author_user_id,work_date) VALUES(?,?,?,?)", log, enterprise, me, today);
        db.update("INSERT INTO work_log_tag(enterprise_id,work_log_id,tag_id) VALUES(?,?,?)", enterprise, log, tag);
        entry(log, 0, "和李小龙联调接口，电话13800138000，邮箱li@example.com，详见 https://intra.example/a");
        entry(log, 1, "完成审批页面开发与自测");
        task(me, me, "整理李小龙提出的需求", today, "ACTIVE");
        task(outsider, outsider, "王五六的私人任务", today, "ACTIVE");
        db.update("INSERT INTO company_important_item(id,enterprise_id,publisher_user_id,title,content) VALUES(?,?,?,?,?)",
                BusinessDb.id(), enterprise, me, "年底上线新版本", "详细内容不发送");
    }

    private String user(String login, String name) {
        String id = BusinessDb.id();
        db.update("INSERT INTO app_user(id,enterprise_id,login_name,password_hash,display_name) VALUES(?,?,?,?,?)", id, enterprise, login, "x", name);
        db.update("INSERT INTO user_role(enterprise_id,user_id,role_code) VALUES(?,?,?)", enterprise, id, "EMPLOYEE");
        return id;
    }

    private String session(String user) {
        String token = UUID.randomUUID().toString();
        db.update("INSERT INTO auth_session(token_hash,enterprise_id,user_id,expires_at) VALUES(?,?,?,?)",
                IdentityService.hash(token), enterprise, user, OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));
        return token;
    }

    private void entry(String log, int order, String content) {
        db.update("INSERT INTO work_log_entry(id,enterprise_id,author_user_id,work_log_id,client_entry_key,content,display_order) VALUES(?,?,?,?,?,?,?)",
                BusinessDb.id(), enterprise, me, log, "k" + order, content, order);
    }

    private void task(String creator, String recipient, String title, LocalDate date, String status) {
        String id = BusinessDb.id();
        db.update("INSERT INTO task(id,enterprise_id,creator_user_id,task_kind,title,priority_level,start_date,end_date,time_kind,review_status) VALUES(?,?,?,?,?,?,?,?,?,?)",
                id, enterprise, creator, "PERSONAL", title, 4, date.minusDays(2), date.minusDays(1), "ALL_DAY", "APPROVED");
        db.update("INSERT INTO task_assignment(id,enterprise_id,task_id,recipient_user_id,execution_status) VALUES(?,?,?,?,?)",
                BusinessDb.id(), enterprise, id, recipient, status);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> call(String method, String path, String token, Object body, int expected) throws Exception {
        var builder = MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method), "/api/v1" + path);
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (body != null) builder.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        var response = mvc.perform(builder).andReturn().getResponse();
        String text = response.getContentAsString(StandardCharsets.UTF_8);
        assertEquals(expected, response.getStatus(), text);
        var parsed = json.readValue(text, Map.class);
        return (Map<String, Object>) (expected < 400 ? parsed.get("data") : parsed.get("error"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> data(Map<String, Object> result) {
        return (Map<String, Object>) result.get("data");
    }

    private long businessRows() {
        long total = 0;
        for (String table : List.of("work_log", "work_log_entry", "task", "task_assignment", "company_important_item", "notification", "operation_log"))
            total += BusinessDb.number(db.one("SELECT COUNT(*) AS c FROM " + table + " WHERE enterprise_id=?", enterprise), "c");
        return total;
    }

    @Test
    void endpointsRequireSession() throws Exception {
        for (String path : List.of("/ai/fortune", "/ai/personality/questions")) call("GET", path, null, null, 401);
        call("POST", "/ai/career-map", null, null, 401);
        assertTrue(REQUESTS.isEmpty());
    }

    @Test
    void careerMapSendsOnlyRedactedDigest() throws Exception {
        long before = businessRows();
        var result = call("POST", "/ai/career-map", myToken, null, 200);
        assertEquals(true, result.get("aiGenerated"));
        var data = data(result);
        assertEquals("全栈探索者", data.get("label"));
        assertEquals(3, ((List<?>) data.get("routes")).size());
        assertEquals("HIGH", ((Map<?, ?>) ((List<?>) data.get("abilities")).get(0)).get("level"));
        assertEquals(1, REQUESTS.size());
        String sent = REQUESTS.get(0);
        for (String secret : List.of("李小龙", "张三丰", "lixl", "13800138000", "li@example.com", "intra.example", "fixture-key"))
            assertFalse(sent.contains(secret), secret);
        assertTrue(sent.contains("同事"));
        assertTrue(sent.contains("Android开发"));
        assertEquals(before, businessRows());
    }

    @Test
    void companyOutlookUsesOnlyVisibleData() throws Exception {
        var result = call("POST", "/ai/company-outlook", myToken, null, 200);
        var stats = (Map<?, ?>) data(result).get("stats");
        assertEquals(1, ((Number) stats.get("taskCount")).intValue());
        assertEquals(1, ((Number) stats.get("overdueCount")).intValue());
        assertEquals(3, ((List<?>) data(result).get("scenarios")).size());
        String sent = REQUESTS.get(0);
        assertTrue(sent.contains("年底上线新版本"));
        assertFalse(sent.contains("详细内容不发送"));
        assertFalse(sent.contains("王五六"));
        assertFalse(sent.contains("整理"));
    }

    @Test
    void emptyCareerMapDoesNotCallModel() throws Exception {
        var result = call("POST", "/ai/career-map", outsiderToken, null, 200);
        // 外部用户只有一项本人任务，仍可生成；删除后为空
        assertEquals(true, result.get("aiGenerated"));
        db.update("DELETE FROM task_assignment WHERE enterprise_id=? AND recipient_user_id=?", enterprise, outsider);
        REQUESTS.clear();
        var empty = call("POST", "/ai/career-map", outsiderToken, null, 200);
        assertEquals(true, data(empty).get("empty"));
        assertEquals(false, empty.get("aiGenerated"));
        assertTrue(REQUESTS.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void personalityIsDeterministicAndValidated() throws Exception {
        var questions = (List<Map<String, Object>>) call("GET", "/ai/personality/questions", myToken, null, 200).get("questions");
        assertEquals(12, questions.size());
        List<String> answers = new ArrayList<>();
        for (var q : questions) answers.add((String) ((List<Map<String, Object>>) q.get("options")).get(0).get("id"));
        var first = data(call("POST", "/ai/personality", myToken, Map.of("optionIds", answers), 200));
        var second = data(call("POST", "/ai/personality", myToken, Map.of("optionIds", answers), 200));
        assertEquals(first.get("archetype"), second.get("archetype"));
        assertEquals(first.get("dimensions"), second.get("dimensions"));
        assertEquals("STRATEGIST", ((Map<?, ?>) first.get("archetype")).get("key"));
        call("POST", "/ai/personality", myToken, Map.of("optionIds", answers.subList(0, 11)), 422);
        var duplicated = new ArrayList<>(answers);
        duplicated.set(11, answers.get(0));
        call("POST", "/ai/personality", myToken, Map.of("optionIds", duplicated), 422);
        var unknown = new ArrayList<>(answers);
        unknown.set(0, "q01-z");
        call("POST", "/ai/personality", myToken, Map.of("optionIds", unknown), 422);
    }

    @Test
    @SuppressWarnings("unchecked")
    void tarotDrawsThreeDistinctCards() throws Exception {
        var result = call("POST", "/ai/tarot", myToken, Map.of("question", "我最近适合尝试新挑战吗？"), 200);
        var cards = (List<Map<String, Object>>) data(result).get("cards");
        assertEquals(3, cards.size());
        assertEquals(3, cards.stream().map(c -> c.get("number")).distinct().count());
        assertEquals(List.of("过去", "现在", "未来"), cards.stream().map(c -> c.get("position")).toList());
        assertEquals("r2", cards.get(1).get("reading"));
        call("POST", "/ai/tarot", myToken, Map.of("question", "长".repeat(101)), 422);
    }

    @Test
    void fortuneIsStableForTheDay() throws Exception {
        var first = call("GET", "/ai/fortune?zodiac=leo", myToken, null, 200);
        var second = call("GET", "/ai/fortune?zodiac=LEO", myToken, null, 200);
        assertEquals(data(first), data(second));
        assertEquals("狮子座", data(first).get("zodiacName"));
        assertEquals(1, REQUESTS.size());
        int stars = ((Number) data(first).get("stars")).intValue();
        assertTrue(stars >= 2 && stars <= 5);
        call("GET", "/ai/fortune?zodiac=dragon", myToken, null, 422);
        assertNotNull(first.get("disclaimer"));
    }
}
