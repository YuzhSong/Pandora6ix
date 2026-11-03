package com.pandora6ix.backend;

import static org.junit.jupiter.api.Assertions.*;

import com.pandora6ix.backend.repository.BusinessDb;
import com.pandora6ix.backend.service.IdentityService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.json.JsonMapper;

/** 日志上级留言：作者信息、留言权限、只删自己的留言、删除日志级联 */
@SpringBootTest
@AutoConfigureMockMvc
class JournalCommentIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired BusinessDb db;
    private String enterprise, journal, leadToken, employeeToken, peerToken;

    @BeforeEach
    void seed() {
        enterprise = BusinessDb.id();
        db.update("INSERT INTO enterprise(id,login_domain,name,business_timezone) VALUES(?,?,?,?)", enterprise, "jc-" + enterprise, "留言测试企业", "Asia/Shanghai");
        String company = org("潘多拉", "COMPANY", null), department = org("研发部", "DEPARTMENT", company), team = org("安卓组", "TEAM", department);
        String lead = user("lead", "陈组长", team, "TEAM_LEAD", "组长");
        String employee = user("employee", "王小明", team, "EMPLOYEE", "Android 工程师");
        String peer = user("peer", "李同事", team, "EMPLOYEE", "测试工程师");
        leadToken = session(lead);
        employeeToken = session(employee);
        peerToken = session(peer);
        journal = BusinessDb.id();
        db.update("INSERT INTO work_log(id,enterprise_id,author_user_id,work_date) VALUES(?,?,?,?)", journal, enterprise, employee, LocalDate.now(ZoneId.of("Asia/Shanghai")));
        db.update("INSERT INTO work_log_entry(id,enterprise_id,author_user_id,work_log_id,client_entry_key,content,display_order) VALUES(?,?,?,?,?,?,?)",
                BusinessDb.id(), enterprise, employee, journal, "k1", "完成审批页面开发", 0);
    }

    private String org(String name, String kind, String parent) {
        String id = BusinessDb.id();
        db.update("INSERT INTO org_unit(id,enterprise_id,name,kind,parent_id) VALUES(?,?,?,?,?)", id, enterprise, name, kind, parent);
        return id;
    }

    private String user(String login, String name, String org, String role, String title) {
        String id = BusinessDb.id();
        db.update("INSERT INTO app_user(id,enterprise_id,login_name,password_hash,display_name,org_unit_id,job_title) VALUES(?,?,?,?,?,?,?)",
                id, enterprise, login, "x", name, org, title);
        db.update("INSERT INTO user_role(enterprise_id,user_id,role_code) VALUES(?,?,?)", enterprise, id, role);
        return id;
    }

    private String session(String user) {
        String token = UUID.randomUUID().toString();
        db.update("INSERT INTO auth_session(token_hash,enterprise_id,user_id,expires_at) VALUES(?,?,?,?)",
                IdentityService.hash(token), enterprise, user, OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));
        return token;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> call(String method, String path, String token, Object body, int expected) throws Exception {
        var builder = MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method), "/api/v1" + path)
                .header("Authorization", "Bearer " + token);
        if (body != null) builder.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        var response = mvc.perform(builder).andReturn().getResponse();
        String text = response.getContentAsString(StandardCharsets.UTF_8);
        assertEquals(expected, response.getStatus(), text);
        var parsed = json.readValue(text, Map.class);
        return (Map<String, Object>) (expected < 400 ? parsed.get("data") : parsed.get("error"));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> items(Map<String, Object> page) {
        return (List<Map<String, Object>>) page.get("items");
    }

    @Test
    void detailIncludesAuthorDepartment() throws Exception {
        var author = (Map<?, ?>) call("GET", "/work-logs/" + journal, leadToken, null, 200).get("author");
        assertEquals("王小明", author.get("displayName"));
        assertEquals("研发部", author.get("departmentName"));
        assertEquals("安卓组", author.get("organizationName"));
        assertEquals("Android 工程师", author.get("jobTitle"));
    }

    @Test
    void managerCommentsAndAuthorReadsOnly() throws Exception {
        var created = call("POST", "/work-logs/" + journal + "/comments", leadToken, Map.of("content", "  进展不错，下周补充测试结果  "), 201);
        assertEquals("进展不错，下周补充测试结果", created.get("content"));
        assertEquals("陈组长", created.get("authorName"));
        assertEquals(List.of("DELETE"), created.get("allowedActions"));
        // 时间为带时区的 ISO 瞬时值，与日志 publishedAt 一致，避免客户端按 UTC 误读本地时间
        var createdAt = java.time.Instant.parse((String) created.get("createdAt"));
        assertTrue(Math.abs(java.time.Duration.between(createdAt, java.time.Instant.now()).toMinutes()) < 5);

        var asAuthor = call("GET", "/work-logs/" + journal + "/comments", employeeToken, null, 200);
        assertEquals(false, asAuthor.get("canComment"));
        assertEquals(1, items(asAuthor).size());
        assertEquals(List.of(), items(asAuthor).get(0).get("allowedActions"));
        assertEquals(true, call("GET", "/work-logs/" + journal + "/comments", leadToken, null, 200).get("canComment"));

        call("POST", "/work-logs/" + journal + "/comments", employeeToken, Map.of("content", "自己评论"), 403);
        call("POST", "/work-logs/" + journal + "/comments", leadToken, Map.of("content", "   "), 422);
        call("POST", "/work-logs/" + journal + "/comments", leadToken, Map.of("content", "长".repeat(501)), 422);
        assertEquals(1, BusinessDb.number(db.one("SELECT COUNT(*) AS c FROM work_log_comment WHERE enterprise_id=?", enterprise), "c"));
    }

    @Test
    void unrelatedColleagueSeesUniformNotFound() throws Exception {
        call("GET", "/work-logs/" + journal + "/comments", peerToken, null, 404);
        call("POST", "/work-logs/" + journal + "/comments", peerToken, Map.of("content", "路过"), 404);
    }

    @Test
    void onlyWriterDeletesAndJournalDeletionCascades() throws Exception {
        String id = (String) call("POST", "/work-logs/" + journal + "/comments", leadToken, Map.of("content", "第一条"), 201).get("id");
        call("DELETE", "/work-log-comments/" + id, employeeToken, null, 403);
        call("DELETE", "/work-log-comments/" + id, peerToken, null, 404);
        assertEquals(id, call("DELETE", "/work-log-comments/" + id, leadToken, null, 200).get("commentId"));
        assertTrue(items(call("GET", "/work-logs/" + journal + "/comments", employeeToken, null, 200)).isEmpty());

        call("POST", "/work-logs/" + journal + "/comments", leadToken, Map.of("content", "第二条"), 201);
        call("DELETE", "/work-logs/" + journal, employeeToken, null, 200);
        assertEquals(0, BusinessDb.number(db.one("SELECT COUNT(*) AS c FROM work_log_comment WHERE enterprise_id=?", enterprise), "c"));
    }
}
