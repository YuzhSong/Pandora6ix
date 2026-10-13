package com.pandora6ix.backend;

import static com.pandora6ix.backend.repository.BusinessDb.*;

import static org.junit.jupiter.api.Assertions.*;

import com.pandora6ix.backend.repository.BusinessDb;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import tools.jackson.databind.json.JsonMapper;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

@SpringBootTest
@AutoConfigureMockMvc
class BusinessApiIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired BusinessDb db;
    private static final String PASSWORD = "integration-password-123";
    private String domain,
            enterprise,
            root,
            department,
            team,
            admin,
            founder,
            manager,
            lead,
            employee,
            peer;
    private String adminToken, founderToken, managerToken, leadToken, employeeToken, peerToken;
    private LocalDate today;

    @BeforeEach
    void setUp() throws Exception {
        domain = "tests-" + UUID.randomUUID();
        today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        var initialized =
                request(
                        "POST",
                        "/admin/enterprise-initializations",
                        null,
                        fields(
                                "loginDomain",
                                domain,
                                "name",
                                "Test enterprise",
                                "businessTimezone",
                                "Asia/Shanghai",
                                "adminLoginName",
                                "admin",
                                "adminPassword",
                                PASSWORD,
                                "adminDisplayName",
                                "Administrator"),
                        201,
                        "X-Bootstrap-Token",
                        "integration-tests-only-bootstrap");
        enterprise = (String) initialized.get("id");
        root = (String) initialized.get("rootOrgUnitId");
        admin = (String) initialized.get("adminUserId");
        adminToken = login("admin");
        department = createOrg("Department", "DEPARTMENT", root);
        team = createOrg("Team", "TEAM", department);
        founder = createUser("founder", root, null, List.of("FOUNDER"));
        manager = createUser("manager", department, founder, List.of("DEPARTMENT_MANAGER"));
        lead = createUser("lead", team, manager, List.of("TEAM_LEAD"));
        employee = createUser("employee", team, lead, List.of("EMPLOYEE"));
        peer = createUser("peer", team, lead, List.of("EMPLOYEE"));
        // Seed credentials directly for most fixtures; login is separately exercised through the
        // public API.
        founderToken = session(founder);
        managerToken = session(manager);
        leadToken = session(lead);
        employeeToken = session(employee);
        peerToken = session(peer);
    }

    private String createOrg(String name, String kind, String parent) throws Exception {
        return (String)
                request(
                                "POST",
                                "/admin/org-units",
                                adminToken,
                                fields("name", name, "kind", kind, "parentId", parent),
                                201)
                        .get("id");
    }

    private String createUser(String login, String org, String supervisor, List<String> roles)
            throws Exception {
        return (String)
                request(
                                "POST",
                                "/admin/users",
                                adminToken,
                                fields(
                                        "loginName",
                                        login,
                                        "password",
                                        PASSWORD,
                                        "displayName",
                                        login,
                                        "orgUnitId",
                                        org,
                                        "supervisorUserId",
                                        supervisor,
                                        "jobTitle",
                                        "Developer",
                                        "roles",
                                        roles),
                                201)
                        .get("id");
    }

    private String login(String name) throws Exception {
        return (String)
                request(
                                "POST",
                                "/auth/login",
                                null,
                                fields(
                                        "enterpriseDomain",
                                        domain,
                                        "loginName",
                                        name,
                                        "password",
                                        PASSWORD),
                                200)
                        .get("accessToken");
    }

    private String session(String user) {
        String token = UUID.randomUUID().toString();
        db.update(
                "INSERT INTO auth_session(token_hash,enterprise_id,user_id,expires_at)"
                        + " VALUES(?,?,?,?)",
                com.pandora6ix.backend.service.IdentityService.hash(token),
                enterprise,
                user,
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));
        return token;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> request(
            String method, String path, String token, Object body, int expected, String... headers)
            throws Exception {
        var result = raw(method, path, token, body, headers);
        assertEquals(expected, result.status(), result.body());
        if (expected == 204) return Map.of();
        var parsed = json.readValue(result.body(), Map.class);
        assertNotNull(parsed.get("requestId"), result.body());
        return expected < 400
                ? (Map<String, Object>) parsed.get("data")
                : (Map<String, Object>) parsed.get("error");
    }

    private record Result(int status, String body) {}

    private Result raw(String method, String path, String token, Object body, String... headers)
            throws Exception {
        MockHttpServletRequestBuilder builder =
                MockMvcRequestBuilders.request(
                                org.springframework.http.HttpMethod.valueOf(method),
                                "/api/v1" + path)
                        .contentType(MediaType.APPLICATION_JSON);
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (body != null) builder.content(json.writeValueAsString(body));
        for (int i = 0; i < headers.length; i += 2) builder.header(headers[i], headers[i + 1]);
        var response = mvc.perform(builder).andReturn().getResponse();
        return new Result(response.getStatus(), response.getContentAsString());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> items(Map<String, Object> page) {
        return (List<Map<String, Object>>) page.get("items");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> entries(Map<String, Object> journal) {
        return (List<Map<String, Object>>) journal.get("entries");
    }

    private Map<String, Object> journal(String token, LocalDate date, int count) throws Exception {
        List<Object> work = new ArrayList<>();
        for (int i = 0; i < count; i++)
            work.add(fields("clientEntryKey", "entry-" + i, "content", "Work content " + i));
        return request(
                "POST",
                "/work-logs",
                token,
                fields(
                        "workDate",
                        date.toString(),
                        "entries",
                        work,
                        "tagIds",
                        List.of(),
                        "primaryTagId",
                        null),
                201);
    }

    private Map<String, Object> taskBody(
            String kind, List<String> recipients, List<String> tags, String primary) {
        return fields(
                "kind",
                kind,
                "title",
                "Task",
                "description",
                "Full task content",
                "priorityLevel",
                3,
                "timeKind",
                "ALL_DAY",
                "startDate",
                today.toString(),
                "endDate",
                today.toString(),
                "recipientIds",
                recipients,
                "tagIds",
                tags,
                "primaryTagId",
                primary);
    }

    private Map<String, Object> createTask(String token, String kind, List<String> recipients)
            throws Exception {
        return request("POST", "/tasks", token, taskBody(kind, recipients, List.of(), null), 201);
    }

    // Approval is RESERVED. This explicit database fixture is not a production API or service
    // bypass.
    private void approved(String task) {
        db.update(
                "UPDATE task SET review_status='APPROVED' WHERE enterprise_id=? AND id=?",
                enterprise,
                task);
        db.update(
                "UPDATE task_assignment SET"
                        + " execution_status='ACTIVE',activated_at=CURRENT_TIMESTAMP WHERE"
                        + " enterprise_id=? AND task_id=?",
                enterprise,
                task);
        db.update(
                "UPDATE task_review SET decision='APPROVED' WHERE enterprise_id=? AND task_id=?",
                enterprise,
                task);
    }

    private String assignment(String task, String recipient) {
        return str(
                db.one(
                        "SELECT id FROM task_assignment WHERE enterprise_id=? AND task_id=? AND"
                                + " recipient_user_id=?",
                        enterprise,
                        task,
                        recipient),
                "id");
    }

    @Test
    void bootstrapAuthenticationAndUnknownCredentialsAreClosed() throws Exception {
        assertEquals(
                "UNAUTHENTICATED",
                request(
                                "POST",
                                "/admin/enterprise-initializations",
                                null,
                                fields(
                                        "loginDomain",
                                        "new-" + UUID.randomUUID(),
                                        "name",
                                        "Another",
                                        "adminLoginName",
                                        "admin",
                                        "adminPassword",
                                        PASSWORD,
                                        "adminDisplayName",
                                        "Admin"),
                                401)
                        .get("code"));
        request("GET", "/me", null, null, 401);
        var bad =
                request(
                        "POST",
                        "/auth/login",
                        null,
                        fields(
                                "enterpriseDomain",
                                domain,
                                "loginName",
                                "unknown",
                                "password",
                                PASSWORD),
                        401);
        assertEquals("INVALID_CREDENTIALS", bad.get("code"));
        var profile = request("GET", "/me", login("employee"), null, 200);
        assertEquals(employee, profile.get("id"));
        assertFalse(json.writeValueAsString(profile).contains("password"));
        assertEquals("Asia/Shanghai", profile.get("businessTimezone"));
    }

    @Test
    void enterpriseInitializationIsIdempotentAndSecretFree() throws Exception {
        var body =
                fields(
                        "loginDomain",
                        "init-" + UUID.randomUUID(),
                        "name",
                        "New enterprise",
                        "businessTimezone",
                        "UTC",
                        "adminLoginName",
                        "first-admin",
                        "adminPassword",
                        PASSWORD,
                        "adminDisplayName",
                        "First administrator");
        var first =
                request(
                        "POST",
                        "/admin/enterprise-initializations",
                        adminToken,
                        body,
                        201,
                        "Idempotency-Key",
                        "initialization-key");
        var retry =
                request(
                        "POST",
                        "/admin/enterprise-initializations",
                        adminToken,
                        body,
                        201,
                        "Idempotency-Key",
                        "initialization-key");
        assertEquals(first, retry);
        assertFalse(json.writeValueAsString(first).contains(PASSWORD));
        body.put("name", "Different");
        request(
                "POST",
                "/admin/enterprise-initializations",
                adminToken,
                body,
                409,
                "Idempotency-Key",
                "initialization-key");
    }

    @Test
    void passwordChangeVerifiesOldSecretRevokesOnlyCurrentSessionAndPreservesData()
            throws Exception {
        String a = login("employee"), b = login("employee");
        var j = journal(a, today, 1);
        request(
                "POST",
                "/me/password",
                a,
                fields("currentPassword", "wrong", "newPassword", "new-integration-password"),
                403);
        request("GET", "/me", a, null, 200);
        request(
                "POST",
                "/me/password",
                a,
                fields("currentPassword", PASSWORD, "newPassword", "new-integration-password"),
                204);
        request("GET", "/me", a, null, 401);
        request("GET", "/work-logs/" + j.get("id"), b, null, 200);
        request(
                "POST",
                "/auth/login",
                null,
                fields("enterpriseDomain", domain, "loginName", "employee", "password", PASSWORD),
                401);
        request(
                "POST",
                "/auth/login",
                null,
                fields(
                        "enterpriseDomain",
                        domain,
                        "loginName",
                        "employee",
                        "password",
                        "new-integration-password"),
                200);
    }

    @Test
    void logoutAndExpiredSessionsCannotReadBusinessData() throws Exception {
        request("POST", "/auth/logout", employeeToken, null, 204);
        request("POST", "/auth/logout", employeeToken, null, 204);
        request("GET", "/me", employeeToken, null, 401);
        db.update(
                "UPDATE auth_session SET expires_at=? WHERE token_hash=?",
                OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1),
                com.pandora6ix.backend.service.IdentityService.hash(peerToken));
        request("GET", "/me", peerToken, null, 401);
    }

    @Test
    void adminDoesNotBecomeABusinessManagerAndCannotForgeIdentity() throws Exception {
        var j = journal(employeeToken, today, 1);
        request("GET", "/work-logs/" + j.get("id"), adminToken, null, 403);
        request("GET", "/work-logs/" + UUID.randomUUID(), adminToken, null, 403);
        request(
                "POST",
                "/tasks",
                adminToken,
                taskBody("PERSONAL", List.of(admin), List.of(), null),
                403);
        var forged = taskBody("PERSONAL", List.of(employee), List.of(), null);
        forged.put("creatorId", founder);
        request("POST", "/tasks", employeeToken, forged, 400);
        request("GET", "/admin/users", employeeToken, null, 403);
    }

    @Test
    void nullJournalEntriesReturnFieldErrorsInsteadOfInternalErrors() throws Exception {
        var result =
                request(
                        "POST",
                        "/work-logs",
                        employeeToken,
                        fields(
                                "workDate",
                                today.toString(),
                                "entries",
                                Arrays.asList((Object) null)),
                        422);
        assertFalse(((List<?>) result.get("fieldErrors")).isEmpty());
    }

    @Test
    void disablingAccountsRevokesOldSessionsAndKeepsAuthorizedHistoricalReads() throws Exception {
        var journal = journal(employeeToken, today, 1);
        var command =
                fields(
                        "expectedVersion",
                        0,
                        "displayName",
                        "employee",
                        "orgUnitId",
                        team,
                        "supervisorUserId",
                        lead,
                        "status",
                        "DISABLED",
                        "roles",
                        List.of("EMPLOYEE"));
        request("PATCH", "/admin/users/" + employee, adminToken, command, 200);
        request("GET", "/me", employeeToken, null, 401);
        request("GET", "/work-logs/" + journal.get("id"), managerToken, null, 200);
        assertFalse(
                items(request("GET", "/tasks/recipient-candidates", managerToken, null, 200))
                        .stream()
                        .anyMatch(row -> employee.equals(row.get("id"))));
        request(
                "POST",
                "/tasks",
                managerToken,
                taskBody("DELEGATED", List.of(employee), List.of(), null),
                403);
        command.put("expectedVersion", 1);
        command.put("status", "ACTIVE");
        request("PATCH", "/admin/users/" + employee, adminToken, command, 200);
        request("GET", "/me", employeeToken, null, 401);
        request("GET", "/me", login("employee"), null, 200);
    }

    @Test
    void seekPaginationIncludesAllJournalCandidatesAndCompanyItems() throws Exception {
        journal(employeeToken, today, 3);
        journal(employeeToken, today.minusDays(1), 3);
        Set<Object> found = new HashSet<>();
        String cursor = null;
        do {
            var page =
                    request(
                            "GET",
                            "/me/important-entry-candidates?limit=2"
                                    + (cursor == null ? "" : "&cursor=" + cursor),
                            employeeToken,
                            null,
                            200);
            for (var row : items(page)) assertTrue(found.add(row.get("id")));
            cursor = (String) page.get("nextCursor");
        } while (cursor != null);
        assertEquals(6, found.size());
        var first = request("GET", "/work-logs?limit=1", employeeToken, null, 200);
        var second =
                request(
                        "GET",
                        "/work-logs?limit=1&cursor=" + first.get("nextCursor"),
                        employeeToken,
                        null,
                        200);
        assertNotEquals(items(first).getFirst().get("id"), items(second).getFirst().get("id"));
        assertNull(second.get("nextCursor"));
        for (int i = 0; i < 3; i++)
            request(
                    "POST",
                    "/company-important-items",
                    adminToken,
                    fields("title", "Item " + i, "content", "Content", "displayOrder", i),
                    201);
        var company = request("GET", "/company-important-items?limit=2", employeeToken, null, 200);
        var last =
                request(
                        "GET",
                        "/company-important-items?limit=2&cursor=" + company.get("nextCursor"),
                        employeeToken,
                        null,
                        200);
        assertEquals(1, items(last).size());
        assertNull(last.get("nextCursor"));
    }

    @Test
    void crossEnterpriseObjectsAndReferencesAreRejectedWithoutLeakage() throws Exception {
        var other =
                request(
                        "POST",
                        "/admin/enterprise-initializations",
                        adminToken,
                        fields(
                                "loginDomain",
                                "other-" + UUID.randomUUID(),
                                "name",
                                "Other",
                                "businessTimezone",
                                "UTC",
                                "adminLoginName",
                                "admin",
                                "adminPassword",
                                PASSWORD,
                                "adminDisplayName",
                                "Admin"),
                        201);
        var item =
                request(
                        "POST",
                        "/company-important-items",
                        adminToken,
                        fields(
                                "title",
                                "Private to enterprise",
                                "content",
                                "Content",
                                "displayOrder",
                                0),
                        201);
        String otherDomain = (String) other.get("loginDomain");
        String otherToken =
                (String)
                        request(
                                        "POST",
                                        "/auth/login",
                                        null,
                                        fields(
                                                "enterpriseDomain",
                                                otherDomain,
                                                "loginName",
                                                "admin",
                                                "password",
                                                PASSWORD),
                                        200)
                                .get("accessToken");
        request("GET", "/company-important-items/" + item.get("id"), otherToken, null, 404);
        request(
                "POST",
                "/admin/users",
                adminToken,
                fields(
                        "loginName",
                        "cross",
                        "password",
                        PASSWORD,
                        "displayName",
                        "Cross",
                        "orgUnitId",
                        other.get("rootOrgUnitId"),
                        "roles",
                        List.of("EMPLOYEE")),
                404);
        assertTrue(
                db.rows(
                                "SELECT id FROM app_user WHERE enterprise_id=? AND"
                                        + " login_name='cross'",
                                enterprise)
                        .isEmpty());
    }

    @Test
    void hierarchyCyclesAndStaleVersionsAreAtomic() throws Exception {
        request(
                "PATCH",
                "/admin/org-units/" + department,
                adminToken,
                fields(
                        "expectedVersion",
                        0,
                        "name",
                        "Department",
                        "kind",
                        "DEPARTMENT",
                        "parentId",
                        team),
                422);
        request(
                "PATCH",
                "/admin/users/" + manager,
                adminToken,
                fields(
                        "expectedVersion",
                        0,
                        "displayName",
                        "Manager",
                        "orgUnitId",
                        department,
                        "supervisorUserId",
                        employee,
                        "status",
                        "ACTIVE",
                        "roles",
                        List.of("DEPARTMENT_MANAGER")),
                422);
        request(
                "PATCH",
                "/admin/users/" + employee,
                adminToken,
                fields(
                        "expectedVersion",
                        0,
                        "displayName",
                        "Changed",
                        "orgUnitId",
                        team,
                        "supervisorUserId",
                        lead,
                        "status",
                        "ACTIVE",
                        "roles",
                        List.of("EMPLOYEE")),
                200);
        request(
                "PATCH",
                "/admin/users/" + employee,
                adminToken,
                fields(
                        "expectedVersion",
                        0,
                        "displayName",
                        "Stale",
                        "orgUnitId",
                        team,
                        "supervisorUserId",
                        lead,
                        "status",
                        "ACTIVE",
                        "roles",
                        List.of("EMPLOYEE")),
                409);
        assertEquals("Changed", db.entity("app_user", enterprise, employee).get("display_name"));
    }

    @Test
    void organizationAndAccountPaginationRemainCompleteAndFilterBound() throws Exception {
        Set<Object> all = new HashSet<>();
        String cursor = null;
        do {
            var page =
                    request(
                            "GET",
                            "/admin/users?limit=2" + (cursor == null ? "" : "&cursor=" + cursor),
                            adminToken,
                            null,
                            200);
            for (var row : items(page)) assertTrue(all.add(row.get("id")));
            cursor = (String) page.get("nextCursor");
        } while (cursor != null);
        assertEquals(6, all.size());
        var page = request("GET", "/admin/users?limit=2", adminToken, null, 200);
        request(
                "GET",
                "/admin/users?q=employee&cursor=" + page.get("nextCursor"),
                adminToken,
                null,
                422);
        request("GET", "/admin/users?limit=101", adminToken, null, 422);
        assertEquals(3, items(request("GET", "/admin/org-units", adminToken, null, 200)).size());
    }

    @Test
    void tagsHonorRoleTenantPrimaryAndAndFiltering() throws Exception {
        var first =
                request(
                        "POST",
                        "/tags",
                        managerToken,
                        fields("name", "Shared", "colorHex", "#12abEF"),
                        201);
        var second =
                request(
                        "POST",
                        "/tags",
                        adminToken,
                        fields("name", "Shared", "colorHex", "#654321"),
                        201);
        request("POST", "/tags", employeeToken, fields("name", "No", "colorHex", "#123456"), 403);
        request(
                "PATCH",
                "/tags/" + first.get("id"),
                managerToken,
                fields("expectedVersion", 0, "name", "No"),
                403);
        request(
                "PATCH",
                "/tags/" + first.get("id"),
                adminToken,
                fields("expectedVersion", 0, "colorHex", "#111111"),
                200);
        request(
                "POST",
                "/tasks",
                employeeToken,
                taskBody("PERSONAL", List.of(employee), List.of((String) first.get("id")), null),
                422);
        var a =
                request(
                        "POST",
                        "/tasks",
                        employeeToken,
                        taskBody(
                                "PERSONAL",
                                List.of(employee),
                                List.of((String) first.get("id"), (String) second.get("id")),
                                (String) second.get("id")),
                        201);
        approved((String) a.get("id"));
        var b =
                request(
                        "POST",
                        "/tasks",
                        employeeToken,
                        taskBody(
                                "PERSONAL",
                                List.of(employee),
                                List.of((String) first.get("id")),
                                (String) first.get("id")),
                        201);
        approved((String) b.get("id"));
        var page =
                request(
                        "GET",
                        "/tasks?tagIds=" + first.get("id") + "," + second.get("id"),
                        employeeToken,
                        null,
                        200);
        assertEquals(1, items(page).size());
        assertEquals("#654321", items(page).getFirst().get("displayColor"));
    }

    @Test
    void recipientsIncludeCrossLevelButExcludePeersAndOtherDepartments() throws Exception {
        String other = createOrg("Other", "DEPARTMENT", root),
                otherTeam = createOrg("Other team", "TEAM", other);
        String outside = createUser("outside", otherTeam, null, List.of("EMPLOYEE"));
        var candidates =
                items(request("GET", "/tasks/recipient-candidates", managerToken, null, 200));
        var ids = candidates.stream().map(r -> r.get("id")).toList();
        assertTrue(ids.containsAll(List.of(lead, employee, peer)));
        assertFalse(ids.contains(outside));
        assertFalse(ids.contains(manager));
        request("GET", "/tasks/recipient-candidates", employeeToken, null, 403);
        request(
                "POST",
                "/tasks",
                managerToken,
                taskBody("DELEGATED", List.of(employee, outside), List.of(), null),
                403);
        assertTrue(db.rows("SELECT id FROM task WHERE enterprise_id=?", enterprise).isEmpty());
        createTask(managerToken, "DELEGATED", List.of(employee, peer));
    }

    @Test
    void tasksArePendingAndUnapprovedAssignmentsCannotExecute() throws Exception {
        var task = createTask(managerToken, "DELEGATED", List.of(employee, peer));
        assertEquals("PENDING", task.get("reviewStatus"));
        String id = (String) task.get("id");
        assertTrue(items(request("GET", "/tasks", employeeToken, null, 200)).isEmpty());
        request("GET", "/tasks/" + id, employeeToken, null, 404);
        request(
                "POST",
                "/task-assignments/" + assignment(id, employee) + "/progress-records",
                employeeToken,
                fields("expectedVersion", 0, "progressPercent", 100),
                409);
        request("GET", "/task-reviews", managerToken, null, 404);
        request("GET", "/inbox", employeeToken, null, 404);
        request(
                "POST",
                "/tasks",
                founderToken,
                taskBody("PERSONAL", List.of(founder), List.of(), null),
                409);
        request(
                "POST",
                "/tasks",
                employeeToken,
                taskBody("PERSONAL", List.of(peer), List.of(), null),
                422);
    }

    @Test
    void pendingEditRefreshesSnapshotAndApprovedEditIsClosed() throws Exception {
        var task = createTask(employeeToken, "PERSONAL", List.of(employee));
        String id = (String) task.get("id");
        var patch =
                fields(
                        "expectedVersion",
                        0,
                        "title",
                        "Changed",
                        "description",
                        "New",
                        "priorityLevel",
                        4,
                        "timeKind",
                        "ALL_DAY",
                        "startDate",
                        today.toString(),
                        "endDate",
                        today.toString(),
                        "tagIds",
                        List.of());
        request("PATCH", "/tasks/" + id, employeeToken, patch, 200);
        var reviews =
                db.rows(
                        "SELECT * FROM task_review WHERE enterprise_id=? AND task_id=? ORDER BY"
                                + " attempt_no",
                        enterprise,
                        id);
        assertEquals(2, reviews.size());
        assertEquals(0, number(reviews.getFirst(), "task_version"));
        assertEquals(1, number(reviews.getLast(), "task_version"));
        assertTrue(str(reviews.getLast(), "submitted_snapshot").contains("Changed"));
        request("PATCH", "/tasks/" + id, employeeToken, patch, 409);
        approved(id);
        patch.put("expectedVersion", 1);
        request("PATCH", "/tasks/" + id, employeeToken, patch, 409);
    }

    @Test
    void pendingEditsRecheckCurrentRecipientsAndSupervisor() throws Exception {
        var delegated = createTask(managerToken, "DELEGATED", List.of(employee));
        String task = (String) delegated.get("id");
        var patch =
                fields(
                        "expectedVersion",
                        0,
                        "title",
                        "Edited",
                        "priorityLevel",
                        3,
                        "timeKind",
                        "ALL_DAY",
                        "startDate",
                        today.toString(),
                        "endDate",
                        today.toString(),
                        "tagIds",
                        List.of());
        db.update(
                "UPDATE app_user SET status='DISABLED' WHERE enterprise_id=? AND id=?",
                enterprise,
                employee);
        request("PATCH", "/tasks/" + task, managerToken, patch, 403);
        assertEquals(0, number(db.entity("task", enterprise, task), "version"));
        var personal = createTask(leadToken, "PERSONAL", List.of(lead));
        String personalId = (String) personal.get("id");
        db.update(
                "UPDATE app_user SET supervisor_user_id=? WHERE enterprise_id=? AND id=?",
                founder,
                enterprise,
                lead);
        request("PATCH", "/tasks/" + personalId, leadToken, patch, 200);
        var snapshots =
                db.rows(
                        "SELECT * FROM task_review WHERE enterprise_id=? AND task_id=? ORDER BY"
                            + " attempt_no",
                        enterprise,
                        personalId);
        assertEquals(manager, snapshots.getFirst().get("reviewer_user_id"));
        assertEquals(founder, snapshots.getLast().get("reviewer_user_id"));
    }

    @Test
    void minuteValidationAndUnconfirmedTimedEndpointsAreDistinct() throws Exception {
        var body = taskBody("PERSONAL", List.of(employee), List.of(), null);
        body.put("timeKind", "TIMED");
        body.put("startTime", "09:15:00");
        body.put("endTime", "10:30");
        request("POST", "/tasks", employeeToken, body, 422);
        body.put("startTime", "09:15");
        request("POST", "/tasks", employeeToken, body, 201);
        body.put("endDate", today.plusDays(1).toString());
        request("POST", "/tasks", employeeToken, body, 409);
        body.put("endDate", today.toString());
        body.put("endTime", "09:15");
        request("POST", "/tasks", employeeToken, body, 409);
    }

    @Test
    void progressIsAppendOnlyAndOneHundredDoesNotComplete() throws Exception {
        var task = createTask(employeeToken, "PERSONAL", List.of(employee));
        String id = (String) task.get("id");
        approved(id);
        String assignment = assignment(id, employee);
        request(
                "POST",
                "/task-assignments/" + assignment + "/progress-records",
                peerToken,
                fields("expectedVersion", 0, "progressPercent", 20),
                404);
        var result =
                request(
                        "POST",
                        "/task-assignments/" + assignment + "/progress-records",
                        employeeToken,
                        fields("expectedVersion", 0, "progressPercent", 100),
                        201);
        assertEquals("ACTIVE", ((Map<?, ?>) result.get("assignment")).get("executionStatus"));
        request(
                "POST",
                "/task-assignments/" + assignment + "/progress-records",
                employeeToken,
                fields("expectedVersion", 0, "progressPercent", 50),
                409);
        assertEquals(
                1,
                db.rows("SELECT id FROM task_progress_record WHERE enterprise_id=?", enterprise)
                        .size());
        request(
                "POST",
                "/tasks/" + id + "/complete",
                employeeToken,
                fields("expectedAssignmentVersion", 1),
                200);
        var row = db.entity("task_assignment", enterprise, assignment);
        assertEquals("SELF", row.get("completion_source"));
        assertEquals(employee, row.get("completed_by_user_id"));
        assertNotNull(row.get("completed_at"));
        request(
                "POST",
                "/tasks/" + id + "/complete",
                employeeToken,
                fields("expectedAssignmentVersion", 2),
                409);
    }

    @Test
    void delegatedCompletionRequiresCreatorAcceptancePerRecipient() throws Exception {
        var task = createTask(managerToken, "DELEGATED", List.of(employee, peer));
        String id = (String) task.get("id");
        approved(id);
        request(
                "POST",
                "/tasks/" + id + "/complete",
                managerToken,
                fields("expectedAssignmentVersion", 0),
                409);
        var submit =
                request(
                        "POST",
                        "/task-assignments/" + assignment(id, employee) + "/completion-requests",
                        employeeToken,
                        fields("expectedVersion", 0, "note", "Done"),
                        201);
        String requestId = (String) ((Map<?, ?>) submit.get("request")).get("id");
        request(
                "POST",
                "/task-completion-requests/" + requestId + "/decision",
                leadToken,
                fields("expectedVersion", 0, "decision", "ACCEPT"),
                404);
        request(
                "POST",
                "/task-completion-requests/" + requestId + "/decision",
                managerToken,
                fields("expectedVersion", 0, "decision", "ACCEPT"),
                200);
        assertEquals(
                "CREATOR_ACCEPTANCE",
                db.entity("task_assignment", enterprise, assignment(id, employee))
                        .get("completion_source"));
        assertEquals(
                "ACTIVE",
                db.entity("task_assignment", enterprise, assignment(id, peer))
                        .get("execution_status"));
        request(
                "POST",
                "/task-completion-requests/" + requestId + "/decision",
                managerToken,
                fields("expectedVersion", 1, "decision", "ACCEPT"),
                409);
    }

    @Test
    void rejectedAcceptanceReactivatesAssignmentWithoutRewritingHistory() throws Exception {
        var task = createTask(managerToken, "DELEGATED", List.of(employee));
        String id = (String) task.get("id");
        approved(id);
        String assignment = assignment(id, employee);
        var first =
                request(
                        "POST",
                        "/task-assignments/" + assignment + "/completion-requests",
                        employeeToken,
                        fields("expectedVersion", 0),
                        201);
        String requestId = (String) ((Map<?, ?>) first.get("request")).get("id");
        request(
                "POST",
                "/task-completion-requests/" + requestId + "/decision",
                managerToken,
                fields("expectedVersion", 0, "decision", "REJECT", "comment", "Needs work"),
                200);
        assertEquals(
                "ACTIVE",
                db.entity("task_assignment", enterprise, assignment).get("execution_status"));
        assertNull(db.entity("task_assignment", enterprise, assignment).get("completed_at"));
        request(
                "POST",
                "/task-assignments/" + assignment + "/completion-requests",
                employeeToken,
                fields("expectedVersion", 2),
                201);
        assertEquals(
                2,
                db.rows("SELECT id FROM task_completion_request WHERE enterprise_id=?", enterprise)
                        .size());
    }

    @Test
    void idempotencyReplaysWithoutDuplicatingHistoryAndRejectsDifferentBodies() throws Exception {
        var body = taskBody("PERSONAL", List.of(employee), List.of(), null);
        var first =
                request("POST", "/tasks", employeeToken, body, 201, "Idempotency-Key", "task-key");
        var replay =
                request("POST", "/tasks", employeeToken, body, 201, "Idempotency-Key", "task-key");
        assertEquals(first.get("id"), replay.get("id"));
        body.put("title", "Different");
        assertEquals(
                "IDEMPOTENCY_CONFLICT",
                request("POST", "/tasks", employeeToken, body, 409, "Idempotency-Key", "task-key")
                        .get("code"));
        String id = (String) first.get("id");
        approved(id);
        String assignment = assignment(id, employee);
        var progress = fields("expectedVersion", 0, "progressPercent", 100);
        request(
                "POST",
                "/task-assignments/" + assignment + "/progress-records",
                employeeToken,
                progress,
                201,
                "Idempotency-Key",
                "progress-key");
        request(
                "POST",
                "/task-assignments/" + assignment + "/progress-records",
                employeeToken,
                progress,
                201,
                "Idempotency-Key",
                "progress-key");
        assertEquals(
                1,
                db.rows("SELECT id FROM task_progress_record WHERE enterprise_id=?", enterprise)
                        .size());
        var complete = fields("expectedAssignmentVersion", 1);
        var completed =
                request(
                        "POST",
                        "/tasks/" + id + "/complete",
                        employeeToken,
                        complete,
                        200,
                        "Idempotency-Key",
                        "complete-key");
        assertEquals(
                completed.get("completedAt"),
                request(
                                "POST",
                                "/tasks/" + id + "/complete",
                                employeeToken,
                                complete,
                                200,
                                "Idempotency-Key",
                                "complete-key")
                        .get("completedAt"));
    }

    @Test
    void concurrentIdempotentCreationAndJournalPublicationAreAtomic() throws Exception {
        var body =
                fields(
                        "workDate",
                        today.toString(),
                        "entries",
                        List.of(fields("clientEntryKey", "one", "content", "Concurrent")));
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a =
                    executor.submit(
                            () ->
                                    raw(
                                            "POST",
                                            "/work-logs",
                                            employeeToken,
                                            body,
                                            "Idempotency-Key",
                                            "same"));
            var b =
                    executor.submit(
                            () ->
                                    raw(
                                            "POST",
                                            "/work-logs",
                                            employeeToken,
                                            body,
                                            "Idempotency-Key",
                                            "same"));
            assertEquals(201, a.get(30, TimeUnit.SECONDS).status());
            assertEquals(201, b.get(30, TimeUnit.SECONDS).status());
            assertEquals(
                    1,
                    db.rows(
                                    "SELECT id FROM work_log WHERE enterprise_id=? AND"
                                            + " author_user_id=?",
                                    enterprise,
                                    employee)
                            .size());
            var c = executor.submit(() -> raw("POST", "/work-logs", peerToken, body));
            var d = executor.submit(() -> raw("POST", "/work-logs", peerToken, body));
            assertEquals(
                    Set.of(201, 409),
                    Set.of(
                            c.get(30, TimeUnit.SECONDS).status(),
                            d.get(30, TimeUnit.SECONDS).status()));
        }
    }

    @Test
    void calendarUsesCompleteMonthRangeHasNoLogsAndCanPageHistoricalTasks() throws Exception {
        var body = taskBody("PERSONAL", List.of(employee), List.of(), null);
        body.put("startDate", "2020-08-01");
        body.put("endDate", "2020-08-02");
        var first = request("POST", "/tasks", employeeToken, body, 201);
        approved((String) first.get("id"));
        var second = request("POST", "/tasks", employeeToken, body, 201);
        approved((String) second.get("id"));
        var page =
                request(
                        "GET",
                        "/calendar?mode=month&anchorDate=2020-08-15&limit=1",
                        employeeToken,
                        null,
                        200);
        assertEquals("2020-07-27", page.get("rangeStart"));
        assertEquals("2020-09-06", page.get("rangeEnd"));
        assertEquals(2, page.get("totalCount"));
        assertFalse(page.containsKey("logs"));
        var next =
                request(
                        "GET",
                        "/calendar?mode=month&anchorDate=2020-08-15&limit=1&cursor="
                                + page.get("nextCursor"),
                        employeeToken,
                        null,
                        200);
        assertNull(next.get("nextCursor"));
        assertNotEquals(
                ((List<?>) page.get("tasks")).getFirst(), ((List<?>) next.get("tasks")).getFirst());
        request("GET", "/calendar?mode=year&anchorDate=2020-08-15", employeeToken, null, 422);
    }

    @Test
    void journalsAreDailyUniqueStableAndOnlyAuthorEditable() throws Exception {
        var journal = journal(employeeToken, today, 2);
        String id = (String) journal.get("id"),
                entry = (String) entries(journal).getFirst().get("id");
        var duplicate =
                request(
                        "POST",
                        "/work-logs",
                        employeeToken,
                        fields(
                                "workDate",
                                today.toString(),
                                "entries",
                                List.of(
                                        fields(
                                                "clientEntryKey",
                                                "new",
                                                "content",
                                                "Do not merge"))),
                        409);
        assertEquals(id, duplicate.get("existingJournalId"));
        var edited =
                request(
                        "PATCH",
                        "/work-logs/" + id,
                        employeeToken,
                        fields(
                                "expectedVersion",
                                0,
                                "upsertEntries",
                                List.of(
                                        fields("entryId", entry, "content", "Corrected"),
                                        fields("clientEntryKey", "new", "content", "Added"))),
                        200);
        assertEquals(3, entries(edited).size());
        assertEquals(entry, entries(edited).getFirst().get("id"));
        assertEquals(
                "Corrected",
                request("GET", "/work-log-entries/" + entry, employeeToken, null, 200)
                        .get("content"));
        assertFalse(
                request("GET", "/work-log-entries/" + entry, employeeToken, null, 200)
                        .containsKey("entries"));
        request("GET", "/work-logs/" + id, peerToken, null, 404);
        request("GET", "/work-logs/" + id, managerToken, null, 200);
        request(
                "PATCH",
                "/work-logs/" + id,
                managerToken,
                fields("expectedVersion", 1, "upsertEntries", List.of()),
                403);
        request(
                "PATCH",
                "/work-logs/" + id,
                employeeToken,
                fields(
                        "expectedVersion",
                        0,
                        "upsertEntries",
                        List.of(fields("entryId", entry, "content", "Stale"))),
                409);
        assertEquals(
                "Corrected",
                request("GET", "/work-log-entries/" + entry, employeeToken, null, 200)
                        .get("content"));
    }

    @Test
    void invalidJournalPatchRollsBackAllEntriesAndVersion() throws Exception {
        var journal = journal(employeeToken, today, 2);
        String id = (String) journal.get("id"),
                entry = (String) entries(journal).getFirst().get("id");
        request(
                "PATCH",
                "/work-logs/" + id,
                employeeToken,
                fields(
                        "expectedVersion",
                        0,
                        "upsertEntries",
                        List.of(
                                fields("entryId", entry, "content", "Should roll back"),
                                fields(
                                        "entryId",
                                        UUID.randomUUID().toString(),
                                        "content",
                                        "Invalid"))),
                404);
        var latest = request("GET", "/work-logs/" + id, employeeToken, null, 200);
        assertEquals(0, latest.get("version"));
        assertEquals("Work content 0", entries(latest).getFirst().get("content"));
    }

    @Test
    void tenDayCandidatesDefaultSelectionAndManualEmptyArePreserved() throws Exception {
        journal(employeeToken, today.minusDays(10), 1);
        journal(employeeToken, today.minusDays(9), 2);
        journal(employeeToken, today, 2);
        assertEquals(
                4,
                items(request("GET", "/me/important-entry-candidates", employeeToken, null, 200))
                        .size());
        var defaults = request("GET", "/me/important-entries", employeeToken, null, 200);
        assertEquals(4, entries(defaults).size());
        assertEquals(false, defaults.get("hasManualSelection"));
        var empty =
                request(
                        "PUT",
                        "/me/important-entries",
                        employeeToken,
                        fields("expectedVersion", 0, "entryIds", List.of()),
                        200);
        assertTrue(entries(empty).isEmpty());
        assertEquals(true, empty.get("hasManualSelection"));
        assertTrue(
                entries(request("GET", "/me/important-entries", employeeToken, null, 200))
                        .isEmpty());
        request(
                "PUT",
                "/me/important-entries",
                employeeToken,
                fields("expectedVersion", 0, "entryIds", List.of()),
                409);
    }

    @Test
    void moreThanTenCandidatesNeedManualSelectionAndInvalidSaveIsAtomic() throws Exception {
        var published = journal(employeeToken, today, 11);
        var selection = request("GET", "/me/important-entries", employeeToken, null, 200);
        assertEquals(true, selection.get("requiresSelection"));
        assertTrue(entries(selection).isEmpty());
        List<Object> ids = entries(published).stream().map(r -> r.get("id")).toList();
        request(
                "PUT",
                "/me/important-entries",
                employeeToken,
                fields("expectedVersion", 0, "entryIds", ids),
                422);
        request(
                "PUT",
                "/me/important-entries",
                employeeToken,
                fields("expectedVersion", 0, "entryIds", ids.subList(0, 10)),
                200);
        var peerJournal = journal(peerToken, today, 1);
        request(
                "PUT",
                "/me/important-entries",
                employeeToken,
                fields(
                        "expectedVersion",
                        1,
                        "entryIds",
                        List.of(entries(peerJournal).getFirst().get("id"))),
                422);
        assertEquals(
                10,
                entries(request("GET", "/me/important-entries", employeeToken, null, 200)).size());
    }

    @Test
    void expiredExistingSelectionsSurviveButNewExpiredCandidatesAreRejected() throws Exception {
        var journal = journal(employeeToken, today, 2);
        String id = (String) journal.get("id"),
                selected = (String) entries(journal).getFirst().get("id"),
                other = (String) entries(journal).getLast().get("id");
        request(
                "PUT",
                "/me/important-entries",
                employeeToken,
                fields("expectedVersion", 0, "entryIds", List.of(selected)),
                200);
        db.update(
                "UPDATE work_log SET work_date=? WHERE enterprise_id=? AND id=?",
                today.minusDays(10),
                enterprise,
                id);
        var set = request("GET", "/me/important-entries", employeeToken, null, 200);
        assertEquals(1, entries(set).size());
        assertEquals(false, entries(set).getFirst().get("eligible"));
        request(
                "PUT",
                "/me/important-entries",
                employeeToken,
                fields("expectedVersion", 1, "entryIds", List.of(selected)),
                200);
        request(
                "PUT",
                "/me/important-entries",
                employeeToken,
                fields("expectedVersion", 2, "entryIds", List.of(selected, other)),
                422);
    }

    @Test
    void missingTimezoneAndInvalidTimezoneNeverUseClientDefaults() throws Exception {
        request(
                "PATCH",
                "/admin/enterprise",
                adminToken,
                fields("expectedVersion", 0, "businessTimezone", "Wrong/Timezone"),
                422);
        db.update("UPDATE enterprise SET business_timezone=NULL WHERE id=?", enterprise);
        assertEquals(
                "BUSINESS_TIMEZONE_NOT_CONFIGURED",
                request("GET", "/dashboard", employeeToken, null, 409).get("code"));
        request(
                "PATCH",
                "/admin/enterprise",
                adminToken,
                fields("expectedVersion", 0, "businessTimezone", "UTC"),
                200);
        assertEquals(
                "UTC", request("GET", "/me", employeeToken, null, 200).get("businessTimezone"));
    }

    @Test
    void companyItemsDashboardAndStatisticsUseAuthorizedSourceFacts() throws Exception {
        var item =
                request(
                        "POST",
                        "/company-important-items",
                        adminToken,
                        fields(
                                "title",
                                "Company title",
                                "content",
                                "Full content",
                                "displayOrder",
                                0),
                        201);
        String id = (String) item.get("id");
        request(
                "POST",
                "/company-important-items",
                employeeToken,
                fields("title", "No", "content", "No", "displayOrder", 0),
                403);
        request(
                "POST",
                "/company-important-items",
                managerToken,
                fields("title", "Restricted", "content", "Pending scope", "displayOrder", 0),
                409);
        request(
                "PATCH",
                "/company-important-items/" + id,
                founderToken,
                fields(
                        "expectedVersion",
                        0,
                        "title",
                        "Changed",
                        "content",
                        "Full changed content",
                        "displayOrder",
                        1),
                200);
        request(
                "PATCH",
                "/company-important-items/" + id,
                founderToken,
                fields(
                        "expectedVersion",
                        0,
                        "title",
                        "Stale",
                        "content",
                        "Stale",
                        "displayOrder",
                        1),
                409);
        var task = createTask(employeeToken, "PERSONAL", List.of(employee));
        approved((String) task.get("id"));
        request(
                "POST",
                "/tasks/" + task.get("id") + "/complete",
                employeeToken,
                fields("expectedAssignmentVersion", 0),
                200);
        createTask(employeeToken, "PERSONAL", List.of(employee));
        var stats = request("GET", "/statistics", employeeToken, null, 200);
        assertEquals(1, stats.get("total"));
        assertEquals(1, stats.get("completed"));
        assertEquals(0, stats.get("uncompleted"));
        journal(employeeToken, today, 2);
        var dashboard = request("GET", "/dashboard", employeeToken, null, 200);
        assertTrue(
                dashboard
                        .keySet()
                        .containsAll(
                                List.of(
                                        "companyImportantItems",
                                        "companyTasks",
                                        "personalImportantEntries",
                                        "workEntries",
                                        "statistics")));
        assertEquals(2, items((Map<String, Object>) dashboard.get("workEntries")).size());
        assertEquals(
                "Full changed content",
                request("GET", "/company-important-items/" + id, employeeToken, null, 200)
                        .get("content"));
    }
}
