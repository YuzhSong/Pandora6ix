package com.pandora6ix.backend;

import static com.pandora6ix.backend.repository.BusinessDb.*;

import static org.junit.jupiter.api.Assertions.*;

import com.pandora6ix.backend.repository.BusinessDb;
import com.pandora6ix.backend.service.NotificationService;

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
    @Autowired NotificationService notifications;
    @Autowired com.pandora6ix.backend.service.CredentialHandoffService credentialHandoffs;
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
                                        roles, "gender", "MALE", "idNumber", "11010119900101001X", "phoneNumber", "+86 13800000000"),
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

    @Test
    void managerReplacementTransfersPeopleAtomicallyAndPreservesCredentials() throws Exception {
        String oldHash=str(db.entity("app_user",enterprise,manager),"password_hash"), nextHash=str(db.entity("app_user",enterprise,peer),"password_hash");
        var command=fields("expectedVersion",0,"successorUserId",peer,"successorExpectedVersion",1,"successorJobTitle","Department manager","formerJobTitle","Staff");
        request("POST","/admin/users/"+manager+"/manager-replacement",adminToken,command,409);
        assertEquals(manager,str(db.entity("app_user",enterprise,lead),"supervisor_user_id"));
        assertEquals(0,db.rows("SELECT id FROM personnel_change WHERE enterprise_id=?",enterprise).size());
        command.put("successorExpectedVersion",0);
        request("POST","/admin/users/"+manager+"/manager-replacement",employeeToken,command,403);
        var result=request("POST","/admin/users/"+manager+"/manager-replacement",adminToken,command,200);
        assertEquals(3,items(result).size());
        assertEquals(peer,user(manager).get("supervisorUserId")); assertEquals(List.of("EMPLOYEE"),user(manager).get("roles"));
        assertTrue(((List<?>)user(peer).get("roles")).contains("DEPARTMENT_MANAGER")); assertEquals(department,user(peer).get("orgUnitId"));
        assertEquals(founder,user(peer).get("supervisorUserId")); assertEquals(peer,user(lead).get("supervisorUserId"));
        assertEquals(lead,user(employee).get("supervisorUserId"));
        assertEquals(oldHash,str(db.entity("app_user",enterprise,manager),"password_hash"));
        assertEquals(nextHash,str(db.entity("app_user",enterprise,peer),"password_hash"));
        request("GET","/me",managerToken,null,401); request("GET","/me",peerToken,null,401); request("GET","/me",leadToken,null,401);
        assertEquals(3,db.rows("SELECT id FROM personnel_change WHERE enterprise_id=?",enterprise).size());
        request("POST","/admin/users/"+manager+"/departure",adminToken,fields("expectedVersion",1,"departureDate",today.toString()),200);
        assertEquals("DEPARTED",user(manager).get("employmentStatus"));
    }

    @Test
    void managerVacancyIsBlockedEvenWithoutDirectReports() throws Exception {
        db.update("UPDATE app_user SET supervisor_user_id=NULL WHERE enterprise_id=? AND supervisor_user_id=?",enterprise,founder);
        var result=request("POST","/admin/users/"+founder+"/departure",adminToken,fields("expectedVersion",0,"departureDate",today.toString()),409);
        assertTrue(json.writeValueAsString(result).contains("MANAGER_SUCCESSOR_REQUIRED"));
        db.update("UPDATE app_user SET supervisor_user_id=NULL WHERE enterprise_id=? AND supervisor_user_id=?",enterprise,manager);
        request("POST","/admin/users/"+manager+"/departure",adminToken,fields("expectedVersion",0,"departureDate",today.toString()),409);
        request("PATCH","/admin/users/"+manager,adminToken,fields("expectedVersion",0,"displayName","manager","orgUnitId",department,"supervisorUserId",null,"jobTitle","Manager","status","DISABLED","roles",List.of("DEPARTMENT_MANAGER")),409);
        request("POST","/admin/users/"+manager+"/job-change",adminToken,fields("expectedVersion",0,"jobTitle","Staff","orgUnitId",department,"supervisorUserId",null,"roles",List.of("EMPLOYEE")),409);
        assertEquals("ACTIVE",user(manager).get("status"));
        assertEquals(0,db.rows("SELECT id FROM personnel_change WHERE enterprise_id=?",enterprise).size());
    }

    @Test
    void departureSeparatesEmploymentAndRevokesCredentialsWithoutDeletingHistory() throws Exception {
        var before=user(employee);
        String hash=str(db.entity("app_user",enterprise,employee),"password_hash");
        request("POST","/admin/users/"+employee+"/departure",employeeToken,fields("expectedVersion",0,"departureDate",today.toString()),403);
        request("POST","/admin/users/"+employee+"/departure",adminToken,fields("expectedVersion",1,"departureDate",today.toString()),409);
        request("POST","/admin/users/"+employee+"/departure",adminToken,fields("expectedVersion",0,"departureDate",today.plusDays(1).toString()),422);
        var departed=request("POST","/admin/users/"+employee+"/departure",adminToken,fields("expectedVersion",0,"departureDate",today.toString()),200);
        assertEquals("DEPARTED",departed.get("employmentStatus")); assertEquals("DISABLED",departed.get("status"));
        assertEquals(today.toString(),departed.get("departureDate")); assertEquals(before.get("idNumber"),departed.get("idNumber"));
        assertEquals(hash,str(db.entity("app_user",enterprise,employee),"password_hash"));
        request("GET","/me",employeeToken,null,401);
        request("POST","/auth/login",null,fields("enterpriseDomain",domain,"loginName","employee","password",PASSWORD),401);
        assertTrue(db.rows("SELECT * FROM credential_handoff WHERE enterprise_id=? AND user_id=?",enterprise,employee).isEmpty());
        request("POST","/admin/users/"+employee+"/password-reset",adminToken,fields("expectedVersion",1,"newPassword",PASSWORD),409);
        request("PATCH","/admin/users/"+employee,adminToken,fields("expectedVersion",1,"displayName","employee","orgUnitId",team,"supervisorUserId",lead,"jobTitle","Developer","status","ACTIVE","roles",List.of("EMPLOYEE")),409);
        request("POST","/admin/users/"+employee+"/job-change",adminToken,fields("expectedVersion",1,"jobTitle","Lead","orgUnitId",team,"supervisorUserId",manager,"roles",List.of("TEAM_LEAD")),409);
        var changes=request("GET","/admin/users/"+employee+"/personnel-changes",adminToken,null,200);
        String serialized=json.writeValueAsString(changes);
        assertTrue(serialized.contains("DEPARTURE")); assertFalse(serialized.contains("password")); assertFalse(serialized.contains("idNumber"));
        request("GET","/admin/users/"+employee+"/personnel-changes",peerToken,null,403);
        String other=UUID.randomUUID().toString();
        request("POST","/admin/users/"+other+"/departure",adminToken,fields("expectedVersion",0,"departureDate",today.toString()),404);
        request("GET","/admin/users/"+other+"/personnel-changes",adminToken,null,404);
        String foreignEnterprise=UUID.randomUUID().toString(), foreignUser=UUID.randomUUID().toString();
        db.update("INSERT INTO enterprise(id,login_domain,name) VALUES(?,?,?)",foreignEnterprise,"foreign-"+foreignEnterprise,"Other company");
        db.update("INSERT INTO app_user(id,enterprise_id,login_name,password_hash,display_name) VALUES(?,?,?,?,?)",foreignUser,foreignEnterprise,"foreign",hash,"Other employee");
        request("GET","/admin/users/"+foreignUser+"/personnel-changes",adminToken,null,404);
        request("POST","/admin/users/"+foreignUser+"/departure",adminToken,fields("expectedVersion",0,"departureDate",today.toString()),404);
        request("POST","/admin/users/"+foreignUser+"/job-change",adminToken,fields("expectedVersion",0,"jobTitle","changed","roles",List.of("EMPLOYEE")),404);
        assertEquals("IN_SERVICE",db.entity("app_user",foreignEnterprise,foreignUser).get("employment_status"));
    }

    @Test
    void jobChangeKeepsAccountAndProfileAndValidatesReporting() throws Exception {
        var before=user(employee);
        String hash=str(db.entity("app_user",enterprise,employee),"password_hash");
        request("POST","/admin/users/"+employee+"/job-change",adminToken,fields("expectedVersion",0,"jobTitle","Lead","orgUnitId",team,"supervisorUserId",lead,"roles",List.of("TEAM_LEAD")),422);
        var changed=request("POST","/admin/users/"+employee+"/job-change",adminToken,fields("expectedVersion",0,"jobTitle","Team manager","orgUnitId",team,"supervisorUserId",manager,"roles",List.of("TEAM_LEAD")),200);
        assertEquals(before.get("loginName"),changed.get("loginName")); assertEquals(before.get("idNumber"),changed.get("idNumber"));
        assertEquals(before.get("phoneNumber"),changed.get("phoneNumber")); assertEquals("IN_SERVICE",changed.get("employmentStatus"));
        assertEquals(hash,str(db.entity("app_user",enterprise,employee),"password_hash"));
        request("GET","/me",employeeToken,null,401);
        assertNotNull(login("employee"));
        var history=request("GET","/admin/users/"+employee+"/personnel-changes",adminToken,null,200);
        assertTrue(json.writeValueAsString(history).contains("JOB_CHANGE"));
        var operationPage=request("GET","/admin/operation-logs?module=PERSONNEL&action=JOB_CHANGE",adminToken,null,200);
        assertEquals(1,items(operationPage).size());
        assertEquals(1,operationPage.get("total"));
        assertNotNull(operationPage.get("snapshotToken"));
        var operation=items(operationPage).getFirst();
        assertEquals("调整员工职位",operation.get("summary"));
        var detail=request("GET","/admin/operation-logs/"+operation.get("id"),adminToken,null,200);
        String serializedOperation=json.writeValueAsString(detail);
        assertTrue(serializedOperation.contains("Team manager"));
        assertFalse(serializedOperation.contains("11010119900101001X"));
        assertFalse(serializedOperation.contains("+86 13800000000"));
        request("GET","/admin/operation-logs?module=ACCOUNT&snapshotToken="+operationPage.get("snapshotToken"),adminToken,null,422);
        request("GET","/admin/operation-logs",peerToken,null,403);
        request("POST","/admin/users/"+lead+"/job-change",adminToken,fields("expectedVersion",0,"jobTitle","Member","orgUnitId",team,"supervisorUserId",manager,"roles",List.of("EMPLOYEE")),409);
        request("POST","/admin/users/"+lead+"/departure",adminToken,fields("expectedVersion",0,"departureDate",today.toString()),409);
        request("POST","/admin/users/"+admin+"/departure",adminToken,fields("expectedVersion",0,"departureDate",today.toString()),409);
        assertEquals(0,db.rows("SELECT * FROM personnel_change WHERE enterprise_id=? AND user_id=?",enterprise,lead).size());
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

    /** 二进制响应（头像）：用 base64 承载字节，避免 String 转换破坏内容 */
    private Result rawBytes(String method, String path, String token) throws Exception {
        MockHttpServletRequestBuilder builder =
                MockMvcRequestBuilders.request(
                        org.springframework.http.HttpMethod.valueOf(method), "/api/v1" + path);
        if (token != null) builder.header("Authorization", "Bearer " + token);
        var response = mvc.perform(builder).andReturn().getResponse();
        return new Result(
                response.getStatus(),
                Base64.getEncoder().encodeToString(response.getContentAsByteArray()));
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
        var created = request(
                "POST",
                "/work-logs",
                token,
                fields(
                        "workDate",
                        (date.isBefore(today.minusDays(2)) ? today : date).toString(),
                        "entries",
                        work,
                        "tagIds",
                        List.of(),
                        "primaryTagId",
                        null),
                201);
        if (date.isBefore(today.minusDays(2))) {
            db.update("UPDATE work_log SET work_date=? WHERE id=?", date, created.get("id"));
            return request("GET", "/work-logs/" + created.get("id"), token, null, 200);
        }
        return created;
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

    // Legacy fixture isolates execution tests at assignment version zero. New lifecycle tests
    // below exercise approval through the public API and use its advanced assignment versions.
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

    private Map<String, Object> reviewDecision(
            String token, Map<String, Object> task, String decision) throws Exception {
        return request(
                "POST",
                "/task-reviews/" + task.get("reviewId") + "/decision",
                token,
                fields("expectedVersion", 0, "decision", decision, "comment", "Reviewed"),
                200);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> taskAssignments(Map<String, Object> task) {
        return (List<Map<String, Object>>) task.get("authorizedAssignments");
    }

    @Test
    void androidCapabilitiesReflectBusinessRolesWithoutGrantingAdminAuthority() throws Exception {
        var employeeProfile = request("GET", "/me", employeeToken, null, 200);
        var employeeCaps = (Map<?, ?>) employeeProfile.get("capabilities");
        assertEquals(true, employeeCaps.get("createPersonalTasks"));
        assertEquals(false, employeeCaps.get("createDelegatedTasks"));
        assertEquals(false, employeeCaps.get("reviewTasks"));
        var managerCaps =
                (Map<?, ?>) request("GET", "/me", managerToken, null, 200).get("capabilities");
        assertEquals(true, managerCaps.get("createDelegatedTasks"));
        assertEquals(true, managerCaps.get("reviewTasks"));
        var adminCaps =
                (Map<?, ?>) request("GET", "/me", adminToken, null, 200).get("capabilities");
        assertEquals(false, adminCaps.get("createPersonalTasks"));
        assertEquals(false, adminCaps.get("reviewTasks"));
    }

    @Test
    void realPersonalApprovalProgressAndCompletionUseReturnedRelationshipVersions()
            throws Exception {
        var task = createTask(employeeToken, "PERSONAL", List.of());
        String reviewId = str(task, "reviewId"), taskId = str(task, "id");
        assertEquals("PENDING", task.get("reviewStatus"));
        assertEquals(0, items(request("GET", "/tasks", employeeToken, null, 200)).size());
        var pending = request("GET", "/task-reviews/" + reviewId, leadToken, null, 200);
        assertEquals(List.of("APPROVE", "REJECT"), pending.get("allowedActions"));
        var decision = reviewDecision(leadToken, task, "APPROVE");
        var review = (Map<?, ?>) decision.get("review");
        assertEquals("APPROVED", review.get("decision"));
        assertEquals(lead, review.get("decidedByUserId"));
        assertEquals("Reviewed", review.get("decisionNote"));
        assertNotNull(review.get("decidedAt"));
        var live = request("GET", "/tasks/" + taskId, employeeToken, null, 200);
        assertEquals(reviewId, live.get("reviewId"));
        assertEquals(1, ((Number) live.get("version")).intValue());
        var relation = taskAssignments(live).getFirst();
        assertEquals("ACTIVE", relation.get("executionStatus"));
        assertNotNull(relation.get("activatedAt"));
        assertEquals(
                List.of("UPDATE_PROGRESS", "COMPLETE_PERSONAL"), relation.get("allowedActions"));
        String assignment = str(relation, "id");
        request(
                "POST",
                "/task-assignments/" + assignment + "/progress-records",
                employeeToken,
                fields("expectedVersion", 0, "progressPercent", 100),
                409);
        var progress =
                request(
                        "POST",
                        "/task-assignments/" + assignment + "/progress-records",
                        employeeToken,
                        fields("expectedVersion", relation.get("version"), "progressPercent", 100),
                        201);
        assertEquals("ACTIVE", ((Map<?, ?>) progress.get("assignment")).get("executionStatus"));
        assertEquals(
                0,
                ((Number) request("GET", "/statistics", employeeToken, null, 200).get("completed"))
                        .intValue());
        var complete =
                request(
                        "POST",
                        "/tasks/" + taskId + "/complete",
                        employeeToken,
                        fields(
                                "expectedAssignmentVersion",
                                ((Map<?, ?>) progress.get("assignment")).get("version")),
                        200);
        assertEquals("SELF", complete.get("completionSource"));
        assertEquals(
                1,
                ((Number) request("GET", "/statistics", employeeToken, null, 200).get("completed"))
                        .intValue());
    }

    @Test
    void realDelegatedApprovalAndPerRecipientAcceptanceDoNotCompleteOtherRecipients()
            throws Exception {
        var task = createTask(leadToken, "DELEGATED", List.of(employee, peer));
        String id = str(task, "id");
        reviewDecision(managerToken, task, "APPROVE");
        var forEmployee = request("GET", "/tasks/" + id, employeeToken, null, 200);
        assertEquals(1, taskAssignments(forEmployee).size());
        var assignment = taskAssignments(forEmployee).getFirst();
        assertEquals(
                List.of("UPDATE_PROGRESS", "REQUEST_COMPLETION"), assignment.get("allowedActions"));
        var requested =
                request(
                        "POST",
                        "/task-assignments/" + assignment.get("id") + "/completion-requests",
                        employeeToken,
                        fields("expectedVersion", assignment.get("version")),
                        201);
        String requestId = (String) ((Map<?, ?>) requested.get("request")).get("id");
        var creatorView = request("GET", "/tasks/" + id, leadToken, null, 200);
        for (var relation : taskAssignments(creatorView)) {
            assertEquals(
                    relation.get("recipientUserId").equals(employee)
                            ? List.of("DECIDE_COMPLETION")
                            : List.of(),
                    relation.get("allowedActions"));
        }
        request(
                "POST",
                "/task-completion-requests/" + requestId + "/decision",
                managerToken,
                fields("expectedVersion", 0, "decision", "ACCEPT"),
                404);
        var completion =
                request(
                        "POST",
                        "/task-completion-requests/" + requestId + "/decision",
                        leadToken,
                        fields("expectedVersion", 0, "decision", "ACCEPT"),
                        200);
        assertEquals(
                "CREATOR_ACCEPTANCE",
                ((Map<?, ?>) completion.get("assignment")).get("completionSource"));
        var forPeer = request("GET", "/tasks/" + id, peerToken, null, 200);
        assertEquals("ACTIVE", taskAssignments(forPeer).getFirst().get("executionStatus"));
        assertEquals(
                List.of("UPDATE_PROGRESS", "REQUEST_COMPLETION"),
                taskAssignments(forPeer).getFirst().get("allowedActions"));
    }

    @Test
    void reviewRejectionRetainsAuditButNeverActivatesRecipients() throws Exception {
        var task = createTask(leadToken, "DELEGATED", List.of(employee, peer));
        assertEquals(1, request("GET", "/task-reviews/pending-count", managerToken, null, 200).get("count"));
        var result = reviewDecision(managerToken, task, "REJECT");
        assertEquals(0, request("GET", "/task-reviews/pending-count", managerToken, null, 200).get("count"));
        assertEquals("REJECTED", ((Map<?, ?>) result.get("review")).get("decision"));
        var creatorView = request("GET", "/tasks/" + task.get("id"), leadToken, null, 200);
        assertEquals("REJECTED", creatorView.get("reviewStatus"));
        assertTrue(
                taskAssignments(creatorView).stream()
                        .allMatch(
                                x ->
                                        "PENDING_APPROVAL".equals(x.get("executionStatus"))
                                                && x.get("activatedAt") == null));
        assertEquals(0, items(request("GET", "/tasks", employeeToken, null, 200)).size());
        assertEquals(0, items(request("GET", "/task-reviews", managerToken, null, 200)).size());
        var history = items(request("GET", "/task-reviews?scope=created", leadToken, null, 200));
        assertEquals("Reviewed", history.getFirst().get("decisionNote"));
        request(
                "POST",
                "/task-reviews/" + task.get("reviewId") + "/decision",
                managerToken,
                fields("expectedVersion", 1, "decision", "APPROVE"),
                409);
    }

    @Test
    void reviewSnapshotsStayImmutableAndSupersededAttemptsCannotDecide() throws Exception {
        var task = createTask(employeeToken, "PERSONAL", List.of());
        assertEquals(1, request("GET", "/task-reviews/pending-count", leadToken, null, 200).get("count"));
        String original = str(task, "reviewId");
        var patch = taskBody("PERSONAL", List.of(), List.of(), null);
        patch.remove("kind");
        patch.remove("recipientIds");
        patch.put("expectedVersion", 0);
        patch.put("title", "Revised task");
        var edited = request("PATCH", "/tasks/" + task.get("id"), employeeToken, patch, 200);
        String latest = str(edited, "reviewId");
        assertNotEquals(original, latest);
        var old = request("GET", "/task-reviews/" + original, leadToken, null, 200);
        assertEquals(false, old.get("isCurrent"));
        assertEquals(List.of(), old.get("allowedActions"));
        assertEquals("Task", ((Map<?, ?>) old.get("submittedSnapshot")).get("title"));
        var current = request("GET", "/task-reviews/" + latest, leadToken, null, 200);
        assertEquals("Revised task", ((Map<?, ?>) current.get("submittedSnapshot")).get("title"));
        assertEquals(
                List.of(employee),
                ((Map<?, ?>) current.get("submittedSnapshot")).get("recipientIds"));
        assertFalse(((Map<?, ?>) current.get("submittedSnapshot")).containsKey("allowedActions"));
        var queue = items(request("GET", "/task-reviews", leadToken, null, 200));
        assertEquals(List.of(latest), queue.stream().map(x -> x.get("id")).toList());
        assertEquals(queue.size(), request("GET", "/task-reviews/pending-count", leadToken, null, 200).get("count"));
        request(
                "POST",
                "/task-reviews/" + original + "/decision",
                leadToken,
                fields("expectedVersion", 0, "decision", "APPROVE"),
                409);
        reviewDecision(leadToken, edited, "APPROVE");
        assertEquals(0, request("GET", "/task-reviews/pending-count", leadToken, null, 200).get("count"));
        assertEquals(
                latest,
                request("GET", "/tasks/" + task.get("id"), employeeToken, null, 200)
                        .get("reviewId"));
        assertEquals(
                2,
                items(request("GET", "/task-reviews?scope=created", employeeToken, null, 200))
                        .size());
    }

    @Test
    void reviewReadsAndDecisionsAreRestrictedToDesignatedBusinessParticipants() throws Exception {
        var task = createTask(employeeToken, "PERSONAL", List.of());
        String path = "/task-reviews/" + task.get("reviewId");
        request("GET", path, employeeToken, null, 200);
        for (String unauthorized : List.of(adminToken, peerToken, managerToken)) {
            request("GET", path, unauthorized, null, 404);
            request(
                    "POST",
                    path + "/decision",
                    unauthorized,
                    fields("expectedVersion", 0, "decision", "APPROVE"),
                    404);
        }
        request(
                "POST",
                path + "/decision",
                employeeToken,
                fields("expectedVersion", 0, "decision", "APPROVE"),
                404);
        request("GET", "/task-reviews", employeeToken, null, 403);
        request("GET", "/task-reviews/pending-count", employeeToken, null, 403);
        request("GET", "/task-reviews/pending-count", adminToken, null, 403);
        request("GET", "/task-reviews?scope=invalid", leadToken, null, 422);
        request("POST", path + "/decision", leadToken, fields("decision", "APPROVE"), 422);
        request(
                "POST",
                path + "/decision",
                leadToken,
                fields("expectedVersion", 0, "decision", "ACCEPT"),
                422);
        request(
                "POST",
                path + "/decision",
                leadToken,
                fields("expectedVersion", 9, "decision", "APPROVE"),
                409);
    }

    @Test
    void changedSupervisorOrDisabledRecipientBlocksApprovalWithoutPartialWrites() throws Exception {
        var personal = createTask(employeeToken, "PERSONAL", List.of());
        db.update(
                "UPDATE app_user SET supervisor_user_id=? WHERE enterprise_id=? AND id=?",
                manager,
                enterprise,
                employee);
        var error =
                request(
                        "POST",
                        "/task-reviews/" + personal.get("reviewId") + "/decision",
                        leadToken,
                        fields("expectedVersion", 0, "decision", "APPROVE"),
                        409);
        assertEquals("REVIEW_CONTEXT_CHANGED", error.get("code"));
        assertEquals(
                List.of(),
                request("GET", "/task-reviews/" + personal.get("reviewId"), leadToken, null, 200)
                        .get("allowedActions"));
        var delegated = createTask(managerToken, "DELEGATED", List.of(employee, peer));
        db.update(
                "UPDATE app_user SET status='DISABLED' WHERE enterprise_id=? AND id=?",
                enterprise,
                peer);
        request(
                "POST",
                "/task-reviews/" + delegated.get("reviewId") + "/decision",
                founderToken,
                fields("expectedVersion", 0, "decision", "APPROVE"),
                409);
        assertEquals(
                "PENDING",
                db.entity("task", enterprise, str(delegated, "id")).get("review_status"));
        assertEquals(
                0,
                number(
                        db.entity("task_review", enterprise, str(delegated, "reviewId")),
                        "version"));
        assertTrue(
                taskAssignments(
                                request(
                                        "GET",
                                        "/tasks/" + delegated.get("id"),
                                        managerToken,
                                        null,
                                        200))
                        .stream()
                        .allMatch(x -> "PENDING_APPROVAL".equals(x.get("executionStatus"))));
    }

    @Test
    void reviewDecisionsAreIdempotentAndRejectChangedCommands() throws Exception {
        var task = createTask(employeeToken, "PERSONAL", List.of());
        String path = "/task-reviews/" + task.get("reviewId") + "/decision";
        var body = fields("expectedVersion", 0, "decision", "APPROVE", "comment", "Original");
        var first = request("POST", path, leadToken, body, 200, "Idempotency-Key", "review-retry");
        assertEquals(
                first,
                request("POST", path, leadToken, body, 200, "Idempotency-Key", "review-retry"));
        body.put("decision", "REJECT");
        var error = request("POST", path, leadToken, body, 409, "Idempotency-Key", "review-retry");
        assertEquals("IDEMPOTENCY_CONFLICT", error.get("code"));
        assertEquals(
                1, number(db.entity("task_review", enterprise, str(task, "reviewId")), "version"));
        assertEquals(
                1,
                number(
                        db.entity(
                                "task_assignment",
                                enterprise,
                                assignment(str(task, "id"), employee)),
                        "version"));
    }

    @Test
    void reviewPaginationIsCompleteAndBoundToActorAndScope() throws Exception {
        for (int i = 0; i < 3; i++) createTask(employeeToken, "PERSONAL", List.of());
        var first = request("GET", "/task-reviews?limit=1", leadToken, null, 200);
        String cursor = str(first, "nextCursor");
        assertNotNull(cursor);
        var second = request("GET", "/task-reviews?limit=1&cursor=" + cursor, leadToken, null, 200);
        var third =
                request(
                        "GET",
                        "/task-reviews?limit=1&cursor=" + second.get("nextCursor"),
                        leadToken,
                        null,
                        200);
        assertEquals(
                3,
                new HashSet<>(
                                List.of(
                                        items(first).getFirst().get("id"),
                                        items(second).getFirst().get("id"),
                                        items(third).getFirst().get("id")))
                        .size());
        assertNull(third.get("nextCursor"));
        request("GET", "/task-reviews?scope=created&cursor=" + cursor, leadToken, null, 422);
        request("GET", "/task-reviews?cursor=" + cursor, managerToken, null, 422);
        assertEquals(0, items(request("GET", "/task-reviews", managerToken, null, 200)).size());
    }

    @Test
    void concurrentReviewDecisionsPersistExactlyOneTransition() throws Exception {
        var task = createTask(employeeToken, "PERSONAL", List.of());
        String path = "/task-reviews/" + task.get("reviewId") + "/decision";
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var approve =
                    executor.submit(
                            () -> {
                                start.await();
                                return raw(
                                        "POST",
                                        path,
                                        leadToken,
                                        fields("expectedVersion", 0, "decision", "APPROVE"));
                            });
            var reject =
                    executor.submit(
                            () -> {
                                start.await();
                                return raw(
                                        "POST",
                                        path,
                                        leadToken,
                                        fields("expectedVersion", 0, "decision", "REJECT"));
                            });
            start.countDown();
            var responses =
                    List.of(approve.get(15, TimeUnit.SECONDS), reject.get(15, TimeUnit.SECONDS));
            assertEquals(
                    List.of(200, 409), responses.stream().map(Result::status).sorted().toList());
            var decided = db.entity("task_review", enterprise, str(task, "reviewId"));
            assertEquals(1, number(decided, "version"));
            var relation =
                    db.entity("task_assignment", enterprise, assignment(str(task, "id"), employee));
            assertEquals(
                    "APPROVED".equals(decided.get("decision")) ? "ACTIVE" : "PENDING_APPROVAL",
                    relation.get("execution_status"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void simultaneousPendingEditAndApprovalCannotApproveSupersededContent() throws Exception {
        var task = createTask(employeeToken, "PERSONAL", List.of());
        String taskId = str(task, "id"), reviewId = str(task, "reviewId");
        var patch = taskBody("PERSONAL", List.of(), List.of(), null);
        patch.remove("kind");
        patch.remove("recipientIds");
        patch.put("expectedVersion", 0);
        patch.put("title", "Concurrent edit");
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var edit =
                    executor.submit(
                            () -> {
                                start.await();
                                return raw("PATCH", "/tasks/" + taskId, employeeToken, patch);
                            });
            var decision =
                    executor.submit(
                            () -> {
                                start.await();
                                return raw(
                                        "POST",
                                        "/task-reviews/" + reviewId + "/decision",
                                        leadToken,
                                        fields("expectedVersion", 0, "decision", "APPROVE"));
                            });
            start.countDown();
            var edited = edit.get(15, TimeUnit.SECONDS);
            var decided = decision.get(15, TimeUnit.SECONDS);
            var stored = db.entity("task", enterprise, taskId);
            if (edited.status() == 200) {
                assertEquals(409, decided.status(), decided.body());
                assertEquals("Concurrent edit", stored.get("title"));
                assertEquals("PENDING", stored.get("review_status"));
                assertEquals(
                        "PENDING_APPROVAL",
                        db.entity("task_assignment", enterprise, assignment(taskId, employee))
                                .get("execution_status"));
            } else {
                assertEquals(409, edited.status(), edited.body());
                assertEquals(200, decided.status(), decided.body());
                assertEquals("Task", stored.get("title"));
                assertEquals("APPROVED", stored.get("review_status"));
            }
        } finally {
            executor.shutdownNow();
        }
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
                        null,
                        body,
                        201,
                        "Idempotency-Key",
                        "initialization-key", "X-Bootstrap-Token", "integration-tests-only-bootstrap");
        var retry =
                request(
                        "POST",
                        "/admin/enterprise-initializations",
                        null,
                        body,
                        201,
                        "Idempotency-Key",
                        "initialization-key", "X-Bootstrap-Token", "integration-tests-only-bootstrap");
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
                "initialization-key", "X-Bootstrap-Token", "integration-tests-only-bootstrap");
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
    void journalEditWindowAndEntryDeletion() throws Exception {
        var j = journal(employeeToken, today, 2);
        String id = str(j, "id");
        var entries = (List<?>) j.get("entries");
        String first = str((Map<String, Object>) entries.get(0), "entryId");
        String second = str((Map<String, Object>) entries.get(1), "entryId");
        request("PUT", "/me/important-entries", employeeToken,
                fields("expectedVersion", 0, "entryIds", List.of(first, second)), 200);
        // 在原条目上删改：修改一条 + 删除一条，不追加新条目
        var patched =
                request(
                        "PATCH",
                        "/work-logs/" + id,
                        employeeToken,
                        fields(
                                "expectedVersion",
                                0,
                                "upsertEntries",
                                List.of(fields("entryId", second, "content", "Revised content")),
                                "deleteEntryIds",
                                List.of(first)),
                        200);
        var remain = (List<?>) patched.get("entries");
        assertEquals(1, remain.size());
        assertEquals("Revised content", str((Map<String, Object>) remain.get(0), "content"));
        var selection = request("GET", "/me/important-entries", employeeToken, null, 200);
        assertEquals(1, entries(selection).size());
        assertEquals(2L, ((Number) selection.get("version")).longValue());
        request("PUT", "/me/important-entries", employeeToken,
                fields("expectedVersion", 1, "entryIds", List.of(second)), 409);
        // 同一请求 upsert 与 delete 重叠
        request(
                "PATCH",
                "/work-logs/" + id,
                employeeToken,
                fields(
                        "expectedVersion",
                        1,
                        "upsertEntries",
                        List.of(fields("entryId", second, "content", "x")),
                        "deleteEntryIds",
                        List.of(second)),
                422);
        // 删除不属于该日志的条目
        request(
                "PATCH",
                "/work-logs/" + id,
                employeeToken,
                fields("expectedVersion", 1, "deleteEntryIds", List.of(UUID.randomUUID().toString())),
                422);
        // 非作者删除
        request("DELETE", "/work-logs/" + id, managerToken, null, 403);
        // 公司日期跨日即不可修改或删除，即使刚刚发表
        db.update(
                "UPDATE work_log SET work_date=? WHERE id=?",
                today.minusDays(1),
                id);
        request(
                "PATCH",
                "/work-logs/" + id,
                employeeToken,
                fields("expectedVersion", 1, "deleteEntryIds", List.of(second)),
                409);
        request("DELETE", "/work-logs/" + id, employeeToken, null, 409);
        // 窗口内删除成功，删除后不可读且同日可重建
        db.update(
                "UPDATE work_log SET work_date=? WHERE id=?",
                today,
                id);
        request("DELETE", "/work-logs/" + id, employeeToken, null, 200);
        selection = request("GET", "/me/important-entries", employeeToken, null, 200);
        assertTrue(entries(selection).isEmpty());
        assertEquals(true, selection.get("hasManualSelection"));
        assertEquals(3L, ((Number) selection.get("version")).longValue());
        request("GET", "/work-logs/" + id, employeeToken, null, 404);
        var again = journal(employeeToken, today, 1);
        assertNotEquals(id, str(again, "id"));
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
                        201, "X-Bootstrap-Token", "integration-tests-only-bootstrap");
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
        var task = createTask(employeeToken, "PERSONAL", List.of());
        request("GET", "/task-reviews/" + task.get("reviewId"), otherToken, null, 404);
        request(
                "POST",
                "/task-reviews/" + task.get("reviewId") + "/decision",
                otherToken,
                fields("expectedVersion", 0, "decision", "APPROVE"),
                404);
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
                        List.of("EMPLOYEE"), "gender", "MALE", "idNumber", "11010119900101001X", "phoneNumber", "13800000000"),
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
                        founderToken,
                        fields("name", "Shared", "colorHex", "#654321"),
                        201);
        request("POST", "/tags", employeeToken, fields("name", "No", "colorHex", "#123456"), 403);
        request(
                "PATCH",
                "/tags/" + first.get("id"),
                employeeToken,
                fields("expectedVersion", 0, "name", "No"),
                403);
        request(
                "PATCH",
                "/tags/" + first.get("id"),
                founderToken,
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
        assertTrue(items(request("GET", "/task-reviews", managerToken, null, 200)).isEmpty());
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
    void timedTasksMayCrossDaysWhileIncreasingEndpointsAreRequired() throws Exception {
        var body = taskBody("PERSONAL", List.of(employee), List.of(), null);
        body.put("timeKind", "TIMED");
        body.put("startTime", "09:15:00");
        body.put("endTime", "10:30");
        request("POST", "/tasks", employeeToken, body, 422);
        body.put("startTime", "09:15");
        request("POST", "/tasks", employeeToken, body, 201);
        // 跨天 TIMED 允许：22:30 → 次日 02:15，结束时刻可以早于开始时刻
        body.put("startTime", "22:30");
        body.put("endTime", "02:15");
        body.put("endDate", today.plusDays(1).toString());
        request("POST", "/tasks", employeeToken, body, 201);
        // 同日必须正时长：同刻或不递增按 422 拒绝
        body.put("startTime", "09:15");
        body.put("endTime", "09:15");
        body.put("endDate", today.toString());
        request("POST", "/tasks", employeeToken, body, 422);
        body.put("endTime", "08:00");
        request("POST", "/tasks", employeeToken, body, 422);
    }

    @Test
    void reviewDetailExposesRequestorAndRecipientNames() throws Exception {
        var created = createTask(employeeToken, "PERSONAL", List.of(employee));
        String task = (String) created.get("id");
        var pending = items(request("GET", "/task-reviews?scope=pending", leadToken, null, 200));
        var row = pending.stream().filter(r -> task.equals(str(r, "taskId"))).findFirst().orElseThrow();
        var detail = request("GET", "/task-reviews/" + row.get("id"), leadToken, null, 200);
        // 请求人 / 审批人展示名：displayName 与登录名同名（createUser 用 login 作 displayName）
        assertEquals("employee", str(detail, "submittedByName"));
        assertEquals("lead", str(detail, "reviewerName"));
        var recipients = (List<Map<String, Object>>) detail.get("recipients");
        assertEquals(1, recipients.size());
        assertEquals(employee, str(recipients.getFirst(), "id"));
        assertEquals("employee", str(recipients.getFirst(), "displayName"));
    }

    @Test
    void reviewDetailExecutorsCarryNamesAndFollowSubmittedSnapshot() throws Exception {
        // 派发给两人的申请：执行人逐个返回，且带真实姓名与职位
        var delegated = createTask(leadToken, "DELEGATED", List.of(employee, peer));
        var detail = request("GET", "/task-reviews/" + delegated.get("reviewId"), managerToken, null, 200);
        assertEquals(2, ((List<?>) detail.get("recipients")).size());
        assertEquals(Set.of("employee", "peer"), recipientNames(detail));
        assertTrue(((List<Map<String, Object>>) detail.get("recipients")).stream()
                .allMatch(r -> str(r, "jobTitle") != null));
        // 快照不可变：任务当前执行关系变化后，旧审批仍显示提交当时申请的执行人
        db.update("DELETE FROM task_assignment WHERE enterprise_id=? AND task_id=?", enterprise, str(delegated, "id"));
        assertEquals(
                Set.of("employee", "peer"),
                recipientNames(request("GET", "/task-reviews/" + delegated.get("reviewId"), managerToken, null, 200)));
    }

    @SuppressWarnings("unchecked")
    private static Set<String> recipientNames(Map<String, Object> detail) {
        return ((List<Map<String, Object>>) detail.get("recipients"))
                .stream().map(r -> str(r, "displayName")).collect(java.util.stream.Collectors.toSet());
    }

    @Test
    void avatarUploadsAndServesRealImageBytes() throws Exception {
        String payload = "iVBORw0KGgo=";
        String dataUrl = "data:image/png;base64," + payload;
        request("PUT", "/users/" + employee + "/avatar", employeeToken, fields("dataUrl", dataUrl), 204);
        var stored = rawBytes("GET", "/users/" + employee + "/avatar", employeeToken);
        assertEquals(200, stored.status());
        assertArrayEquals(Base64.getDecoder().decode(payload), Base64.getDecoder().decode(stored.body()));
        // 未设置头像返回 404，客户端回退到姓名首字而不是占位假图
        assertEquals(404, rawBytes("GET", "/users/" + manager + "/avatar", employeeToken).status());
        // 他人头像可读取（审批详情要显示请求人/执行人真实头像），但不能代改
        request("PUT", "/users/" + manager + "/avatar", employeeToken, fields("dataUrl", dataUrl), 403);
        request("PUT", "/users/" + employee + "/avatar", employeeToken, fields("dataUrl", "https://x/a.png"), 422);
    }

    @Test
    void allDayTaskMaySpanMultipleDays() throws Exception {
        var body = taskBody("PERSONAL", List.of(employee), List.of(), null);
        body.put("endDate", today.plusDays(3).toString());
        var created = request("POST", "/tasks", employeeToken, body, 201);
        var stored =
                db.one(
                        "SELECT * FROM task WHERE enterprise_id=? AND id=?",
                        enterprise,
                        created.get("id"));
        assertEquals(today.plusDays(3).toString(), str(stored, "end_date"));
        assertNull(stored.get("start_minute"));
        assertNull(stored.get("end_minute"));
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

    private Map<String, Object> personnelCommand(String name) {
        return fields("loginName", name, "password", PASSWORD, "displayName", name,
                "orgUnitId", team, "supervisorUserId", lead, "jobTitle", "Developer",
                "roles", List.of("EMPLOYEE"), "gender", "FEMALE",
                "idNumber", "11010119900101001X", "phoneNumber", "+86 (010) 1234-5678");
    }

    private Map<String, Object> personnelPatch(String id, long version, List<String> roles) {
        var row = db.entity("app_user", enterprise, id);
        return fields("expectedVersion", version, "displayName", row.get("display_name"),
                "orgUnitId", row.get("org_unit_id"), "supervisorUserId", row.get("supervisor_user_id"),
                "jobTitle", row.get("job_title"), "status", row.get("status"), "roles", roles);
    }

    @Test
    void administratorRenamesLoginWithoutChangingIdentityPasswordProfileOrBusinessRelations() throws Exception {
        var task = createTask(employeeToken, "PERSONAL", List.of());
        var journal = journal(employeeToken, today, 1);
        var before = new LinkedHashMap<>(db.entity("app_user", enterprise, employee));
        var beforeRoles = db.rows("SELECT * FROM user_role WHERE enterprise_id=? AND user_id=?", enterprise, employee);
        var taskBefore = db.entity("task", enterprise, str(task, "id"));
        var journalBefore = db.entity("work_log", enterprise, str(journal, "id"));
        var patch = personnelPatch(employee, 0, List.of("EMPLOYEE"));
        patch.put("loginName", "zhang.san");
        var edited = request("PATCH", "/admin/users/" + employee, adminToken, patch, 200);
        assertEquals(employee, edited.get("id"));
        assertEquals("zhang.san", edited.get("loginName"));
        assertFalse(edited.containsKey("passwordHash"));
        var after = new LinkedHashMap<>(db.entity("app_user", enterprise, employee));
        assertEquals(1, number(after, "version"));
        for (String key : List.of("login_name", "version")) { before.remove(key); after.remove(key); }
        assertEquals(before, after);
        assertEquals(beforeRoles, db.rows("SELECT * FROM user_role WHERE enterprise_id=? AND user_id=?", enterprise, employee));
        assertEquals(taskBefore, db.entity("task", enterprise, str(task, "id")));
        assertEquals(journalBefore, db.entity("work_log", enterprise, str(journal, "id")));
        assertEquals(employee, request("GET", "/me", login("zhang.san"), null, 200).get("id"));
        assertEquals(employee, request("GET", "/me", employeeToken, null, 200).get("id"));
        request("GET", "/tasks/" + task.get("id"), employeeToken, null, 200);
        request("GET", "/work-logs/" + journal.get("id"), employeeToken, null, 200);
        request("POST", "/auth/login", null, fields("enterpriseDomain", domain,
                "loginName", "employee", "password", PASSWORD), 401);
        assertEquals(employee, items(request("GET", "/admin/users?q=zhang.san", adminToken, null, 200)).getFirst().get("id"));
    }

    @Test
    void omittedNullAndUnchangedLoginNamesPreserveLegacyUpdates() throws Exception {
        request("PATCH", "/admin/users/" + employee, adminToken,
                personnelPatch(employee, 0, List.of("EMPLOYEE")), 200);
        var patch = personnelPatch(employee, 1, List.of("EMPLOYEE", "ADMIN"));
        patch.put("loginName", null);
        request("PATCH", "/admin/users/" + employee, adminToken, patch, 200);
        patch = personnelPatch(employee, 2, List.of("EMPLOYEE"));
        patch.put("loginName", "employee");
        request("PATCH", "/admin/users/" + employee, adminToken, patch, 200);
        assertEquals("employee", db.entity("app_user", enterprise, employee).get("login_name"));
        assertNotNull(login("employee"));
        assertEquals("VERSION_CONFLICT", request("PATCH", "/admin/users/" + employee,
                adminToken, patch, 409).get("code"));
    }

    @Test
    void invalidDuplicateAndStaleLoginRenamesLeaveTheWholeAccountUntouched() throws Exception {
        var before = db.entity("app_user", enterprise, employee);
        var rolesBefore = db.rows("SELECT * FROM user_role WHERE enterprise_id=? AND user_id=?", enterprise, employee);
        for (String value : List.of("", "   ", "\t\n", "\u2003", " zhang.san", "zhang.san ", "\u2003zhang.san", "x".repeat(129), "peer")) {
            var patch = personnelPatch(employee, 0, List.of("EMPLOYEE", "ADMIN"));
            patch.put("displayName", "Must not save"); patch.put("loginName", value);
            var error = request("PATCH", "/admin/users/" + employee, adminToken, patch, value.equals("peer") ? 409 : 422);
            assertEquals(value.equals("peer") ? "LOGIN_NAME_TAKEN" : "VALIDATION_FAILED", error.get("code"));
            if (!value.equals("peer")) assertTrue(error.get("fieldErrors").toString().contains("loginName"));
            assertEquals(before, db.entity("app_user", enterprise, employee));
            assertEquals(rolesBefore, db.rows("SELECT * FROM user_role WHERE enterprise_id=? AND user_id=?", enterprise, employee));
        }
        var valid = personnelPatch(employee, 0, List.of("EMPLOYEE")); valid.put("loginName", "x".repeat(128));
        request("PATCH", "/admin/users/" + employee, adminToken, valid, 200);
        valid.put("loginName", "peer");
        assertEquals("VERSION_CONFLICT", request("PATCH", "/admin/users/" + employee, adminToken, valid, 409).get("code"));
    }

    @Test
    void loginRenameIsAdministratorOnlyAndEnterpriseScoped() throws Exception {
        var patch = personnelPatch(employee, 0, List.of("EMPLOYEE")); patch.put("loginName", "local.new");
        request("PATCH", "/admin/users/" + employee, employeeToken, patch, 403);
        request("PATCH", "/admin/users/" + employee, managerToken, patch, 403);
        request("PATCH", "/admin/users/" + employee, null, patch, 401);
        var other = request("POST", "/admin/enterprise-initializations", null,
                fields("loginDomain", "rename-" + UUID.randomUUID(), "name", "Other", "businessTimezone", "UTC",
                        "adminLoginName", "foreign.login", "adminPassword", PASSWORD, "adminDisplayName", "Other admin"),
                201, "X-Bootstrap-Token", "integration-tests-only-bootstrap");
        String otherId = str(other, "adminUserId");
        var otherBefore = db.entity("app_user", str(other, "id"), otherId);
        request("PATCH", "/admin/users/" + otherId, adminToken, patch, 404);
        var otherLogin = request("POST", "/auth/login", null, fields("enterpriseDomain", other.get("loginDomain"),
                "loginName", "foreign.login", "password", PASSWORD), 200);
        request("PATCH", "/admin/users/" + employee, str(otherLogin, "accessToken"), patch, 404);
        patch.put("loginName", "foreign.login");
        request("PATCH", "/admin/users/" + employee, adminToken, patch, 200);
        assertEquals(otherBefore, db.entity("app_user", str(other, "id"), otherId));
        assertNotNull(login("foreign.login"));
    }

    @Test
    void renamedAdministratorKeepsSessionAndLastAdminProtection() throws Exception {
        var patch = personnelPatch(admin, 0, List.of("ADMIN")); patch.put("loginName", "guanliyuan");
        request("PATCH", "/admin/users/" + admin, adminToken, patch, 200);
        request("GET", "/admin/users", adminToken, null, 200);
        assertEquals(admin, request("GET", "/me", login("guanliyuan"), null, 200).get("id"));
        patch = personnelPatch(admin, 1, List.of("EMPLOYEE")); patch.put("loginName", "must.not.save");
        assertEquals("LAST_ADMIN_REQUIRED", request("PATCH", "/admin/users/" + admin, adminToken, patch, 409).get("code"));
        assertEquals("guanliyuan", db.entity("app_user", enterprise, admin).get("login_name"));
    }

    @Test
    void concurrentRenamesOfOneAccountRejectTheStaleVersion() throws Exception {
        var first = personnelPatch(employee, 0, List.of("EMPLOYEE")); first.put("loginName", "rename.one");
        var second = personnelPatch(employee, 0, List.of("EMPLOYEE")); second.put("loginName", "rename.two");
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> { start.await(); return raw("PATCH", "/admin/users/" + employee, adminToken, first); });
            var b = executor.submit(() -> { start.await(); return raw("PATCH", "/admin/users/" + employee, adminToken, second); });
            start.countDown();
            var results = List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
            assertEquals(List.of(200, 409), results.stream().map(Result::status).sorted().toList());
            assertTrue(results.stream().filter(r -> r.status() == 409).findFirst().orElseThrow().body().contains("VERSION_CONFLICT"));
        }
        assertEquals(1, number(db.entity("app_user", enterprise, employee), "version"));
    }

    @Test
    void concurrentAccountsClaimingOneLoginNameDoNotPartiallyUpdateTheLoser() throws Exception {
        var first = personnelPatch(employee, 0, List.of("EMPLOYEE")); first.put("loginName", "shared.login");
        var second = personnelPatch(peer, 0, List.of("EMPLOYEE")); second.put("loginName", "shared.login");
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> { start.await(); return raw("PATCH", "/admin/users/" + employee, adminToken, first); });
            var b = executor.submit(() -> { start.await(); return raw("PATCH", "/admin/users/" + peer, adminToken, second); });
            start.countDown();
            var results = List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
            assertEquals(List.of(200, 409), results.stream().map(Result::status).sorted().toList());
            assertTrue(results.stream().filter(r -> r.status() == 409).findFirst().orElseThrow().body().contains("LOGIN_NAME_TAKEN"));
        }
        assertEquals(1, db.rows("SELECT id FROM app_user WHERE enterprise_id=? AND login_name='shared.login'", enterprise).size());
        assertEquals(1, number(db.entity("app_user", enterprise, employee), "version") + number(db.entity("app_user", enterprise, peer), "version"));
    }

    @Test
    void personnelProfilePersistsLoginAndRoleEditsWithoutBusinessLeakage() throws Exception {
        var created = request("POST", "/admin/users", adminToken, personnelCommand("profile"), 201);
        String id = str(created, "id");
        assertEquals("FEMALE", created.get("gender"));
        assertEquals("11010119900101001X", created.get("idNumber"));
        assertEquals("+86 (010) 1234-5678", created.get("phoneNumber"));
        assertFalse(created.containsKey("passwordHash"));
        var profile = request("GET", "/me", login("profile"), null, 200);
        for (String key : List.of("gender", "idNumber", "phoneNumber", "passwordHash"))
            assertFalse(profile.containsKey(key));
        var patch = personnelPatch(id, 0, List.of("EMPLOYEE", "ADMIN"));
        patch.put("gender", null); patch.put("idNumber", null); patch.put("phoneNumber", null);
        var edited = request("PATCH", "/admin/users/" + id, adminToken, patch, 200);
        assertEquals(created.get("idNumber"), edited.get("idNumber"));
        assertEquals(created.get("phoneNumber"), edited.get("phoneNumber"));
        assertEquals(created.get("gender"), edited.get("gender"));
        request("PATCH", "/admin/users/" + id, adminToken, patch, 409);
        patch = personnelPatch(id, 1, List.of("EMPLOYEE"));
        patch.put("gender", "MALE"); patch.put("phoneNumber", "021-87654321");
        request("PATCH", "/admin/users/" + id, adminToken, patch, 200);
        var reloaded = items(request("GET", "/admin/users?q=profile", adminToken, null, 200)).getFirst();
        assertEquals("021-87654321", reloaded.get("phoneNumber"));
        assertEquals("MALE", reloaded.get("gender"));
        request("POST", "/admin/users", adminToken, personnelCommand("profile"), 409);
        assertEquals(1, db.rows("SELECT id FROM app_user WHERE enterprise_id=? AND login_name='profile'", enterprise).size());
        assertEquals(1, db.rows("SELECT role_code FROM user_role WHERE enterprise_id=? AND user_id=?", enterprise, id).size());
    }

    @Test
    void legacyProfileNullsAndInvalidPersonnelWritesAreAtomic() throws Exception {
        var legacy = items(request("GET", "/admin/users?q=admin", adminToken, null, 200)).getFirst();
        for (String key : List.of("gender", "idNumber", "phoneNumber")) {
            assertTrue(legacy.containsKey(key)); assertNull(legacy.get(key));
        }
        var edited = request("PATCH", "/admin/users/" + admin, adminToken,
                personnelPatch(admin, 0, List.of("ADMIN")), 200);
        assertNull(edited.get("idNumber"));
        for (String key : List.of("gender", "idNumber", "phoneNumber")) {
            var body = personnelCommand("missing-" + key); body.remove(key);
            request("POST", "/admin/users", adminToken, body, 422);
        }
        for (var invalid : List.of(fields("gender", "UNKNOWN"), fields("idNumber", "bad-id"),
                fields("phoneNumber", "(---)---"), fields("phoneNumber", "12345"))) {
            var body = personnelCommand("invalid"); body.putAll(invalid);
            request("POST", "/admin/users", adminToken, body, 422);
        }
        var body = personnelCommand("invalid-role"); body.put("roles", List.of("TEAM_LEAD", "INVALID"));
        request("POST", "/admin/users", adminToken, body, 422);
        assertTrue(db.rows("SELECT id FROM app_user WHERE enterprise_id=? AND (login_name LIKE 'missing-%' OR login_name IN ('invalid','invalid-role'))", enterprise).isEmpty());
        body = personnelPatch(employee, 0, List.of("ADMIN")); body.put("idNumber", "invalid");
        request("PATCH", "/admin/users/" + employee, adminToken, body, 422);
        assertEquals(0, number(db.entity("app_user", enterprise, employee), "version"));
        assertEquals(List.of("EMPLOYEE"), db.rows("SELECT role_code FROM user_role WHERE enterprise_id=? AND user_id=?", enterprise, employee).stream().map(r -> str(r,"role_code")).toList());
    }

    @Test
    void leafOrganizationConversionKeepsIdAndEnforcesHierarchyVersionsAndRoles() throws Exception {
        String leaf = createOrg("Leaf", "DEPARTMENT", root);
        var converted = request("PATCH", "/admin/org-units/" + leaf, adminToken,
                fields("expectedVersion", 0, "name", "Leaf team", "kind", "TEAM", "parentId", department), 200);
        assertEquals(leaf, converted.get("id")); assertEquals("TEAM", converted.get("kind"));
        request("PATCH", "/admin/org-units/" + leaf, adminToken,
                fields("expectedVersion", 0, "name", "Stale", "kind", "DEPARTMENT", "parentId", root), 409);
        request("PATCH", "/admin/org-units/" + leaf, adminToken,
                fields("expectedVersion", 1, "name", "Leaf", "kind", "DEPARTMENT", "parentId", root), 200);
        request("PATCH", "/admin/org-units/" + root, adminToken,
                fields("expectedVersion", 0, "name", "No", "kind", "DEPARTMENT", "parentId", department), 422);
        request("PATCH", "/admin/org-units/" + department, adminToken,
                fields("expectedVersion", 0, "name", "No", "kind", "TEAM", "parentId", leaf), 422);
        createUser("leaf-manager", leaf, founder, List.of("DEPARTMENT_MANAGER"));
        request("PATCH", "/admin/org-units/" + leaf, adminToken,
                fields("expectedVersion", 2, "name", "No", "kind", "TEAM", "parentId", department), 422);
        request("PATCH", "/admin/org-units/" + team, adminToken,
                fields("expectedVersion", 0, "name", "No", "kind", "DEPARTMENT", "parentId", root), 422);
        request("POST", "/admin/org-units", adminToken,
                fields("name", "Nested department", "kind", "DEPARTMENT", "parentId", department), 422);
        assertEquals("DEPARTMENT", db.entity("org_unit", enterprise, leaf).get("kind"));
    }

    @Test
    void deleteEmptyOrganizationsRequiresCurrentVersionAndPreservesOtherRecords() throws Exception {
        String emptyDepartment = createOrg("Disposable department", "DEPARTMENT", root);
        String emptyTeam = createOrg("Disposable team", "TEAM", emptyDepartment);
        String path = "/admin/org-units/" + emptyTeam;
        assertEquals("VERSION_CONFLICT", request("DELETE", path, adminToken, fields("expectedVersion", 1), 409).get("code"));
        request("DELETE", path, adminToken, fields(), 422);
        request("DELETE", path, adminToken, fields("expectedVersion", -1), 422);
        request("DELETE", path, adminToken, null, 400);
        request("DELETE", path, adminToken, fields("expectedVersion", 0), 204);
        request("DELETE", path, adminToken, fields("expectedVersion", 0), 404);
        request("DELETE", "/admin/org-units/" + emptyDepartment, adminToken, fields("expectedVersion", 0), 204);
        assertEquals(3, items(request("GET", "/admin/org-units", adminToken, null, 200)).size());
        assertEquals(team, db.entity("app_user", enterprise, employee).get("org_unit_id"));
    }

    @Test
    void deleteOrganizationRefusesRootChildrenAndAllAssignedAccounts() throws Exception {
        assertEquals("ORG_ROOT_DELETE_FORBIDDEN", request("DELETE", "/admin/org-units/" + root, adminToken, fields("expectedVersion", 0), 409).get("code"));
        assertEquals("ORG_HAS_CHILDREN", request("DELETE", "/admin/org-units/" + department, adminToken, fields("expectedVersion", 0), 409).get("code"));
        assertEquals("ORG_HAS_MEMBERS", request("DELETE", "/admin/org-units/" + team, adminToken, fields("expectedVersion", 0), 409).get("code"));
        String leaf = createOrg("Disabled account department", "DEPARTMENT", root);
        String disabled = createUser("disabled-delete", leaf, founder, List.of("EMPLOYEE"));
        db.update("UPDATE app_user SET status='DISABLED' WHERE id=?", disabled);
        assertEquals("ORG_HAS_MEMBERS", request("DELETE", "/admin/org-units/" + leaf, adminToken, fields("expectedVersion", 0), 409).get("code"));
        assertNotNull(db.entity("org_unit", enterprise, leaf));
    }

    @Test
    void deleteOrganizationRequiresAdminAndHidesForeignEnterpriseUnits() throws Exception {
        String leaf = createOrg("Private leaf", "DEPARTMENT", root);
        String path = "/admin/org-units/" + leaf;
        request("DELETE", path, null, fields("expectedVersion", 0), 401);
        request("DELETE", path, employeeToken, fields("expectedVersion", 0), 403);
        request("DELETE", path, founderToken, fields("expectedVersion", 0), 403);
        var other = request("POST", "/admin/enterprise-initializations", null,
                fields("loginDomain", "delete-other-" + UUID.randomUUID(), "name", "Other", "adminLoginName", "admin", "adminPassword", PASSWORD, "adminDisplayName", "Admin"),
                201, "X-Bootstrap-Token", "integration-tests-only-bootstrap");
        String otherToken = (String) request("POST", "/auth/login", null,
                fields("enterpriseDomain", other.get("loginDomain"), "loginName", "admin", "password", PASSWORD), 200).get("accessToken");
        assertEquals("RESOURCE_NOT_FOUND", request("DELETE", path, otherToken, fields("expectedVersion", 0), 404).get("code"));
        assertNotNull(db.entity("org_unit", enterprise, leaf));
    }

    @Test
    void concurrentOrganizationDeletionAndChildCreationPreserveHierarchy() throws Exception {
        String leaf = createOrg("Concurrent deletion", "DEPARTMENT", root);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var deletion = executor.submit(() -> { start.await(); return raw("DELETE", "/admin/org-units/" + leaf, adminToken, fields("expectedVersion", 0)); });
            var creation = executor.submit(() -> { start.await(); return raw("POST", "/admin/org-units", adminToken, fields("name", "Concurrent child", "kind", "TEAM", "parentId", leaf)); });
            start.countDown();
            int removed = deletion.get(20, TimeUnit.SECONDS).status();
            int created = creation.get(20, TimeUnit.SECONDS).status();
            assertTrue((removed == 204 && created == 404) || (removed == 409 && created == 201), removed + "/" + created);
            assertEquals(removed == 204, db.rows("SELECT id FROM org_unit WHERE enterprise_id=? AND id=?", enterprise, leaf).isEmpty());
            assertEquals(removed == 204, db.rows("SELECT id FROM org_unit WHERE enterprise_id=? AND parent_id=?", enterprise, leaf).isEmpty());
        }
    }

    @Test
    void concurrentOrganizationDeletionAndPersonnelTransferPreserveMembership() throws Exception {
        String leaf = createOrg("Concurrent transfer", "DEPARTMENT", root);
        var patch = personnelPatch(employee, 0, List.of("EMPLOYEE"));
        patch.put("orgUnitId", leaf);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var deletion = executor.submit(() -> { start.await(); return raw("DELETE", "/admin/org-units/" + leaf, adminToken, fields("expectedVersion", 0)); });
            var transfer = executor.submit(() -> { start.await(); return raw("PATCH", "/admin/users/" + employee, adminToken, patch); });
            start.countDown();
            int removed = deletion.get(20, TimeUnit.SECONDS).status();
            int moved = transfer.get(20, TimeUnit.SECONDS).status();
            assertTrue((removed == 204 && moved == 404) || (removed == 409 && moved == 200), removed + "/" + moved);
            assertEquals(removed == 204 ? team : leaf, db.entity("app_user", enterprise, employee).get("org_unit_id"));
        }
    }

    @Test
    void adminResetsEmployeePasswordAndRevokesEveryOldSessionWithoutChangingProfile() throws Exception {
        String secondSession = login("employee");
        var before = db.entity("app_user", enterprise, employee);
        var result = request("POST", "/admin/users/" + employee + "/password-reset", adminToken,
                fields("expectedVersion", 0, "newPassword", "Employee-new-password"), 200);
        assertEquals(employee, result.get("id"));
        assertEquals(1, ((Number) result.get("version")).intValue());
        assertFalse(result.containsKey("passwordHash")); assertFalse(result.containsKey("password")); assertFalse(result.containsKey("newPassword"));
        var after = db.entity("app_user", enterprise, employee);
        for (String key : List.of("display_name", "login_name", "org_unit_id", "supervisor_user_id", "job_title", "gender", "id_number", "phone_number", "status"))
            assertEquals(before.get(key), after.get(key), key);
        request("GET", "/me", employeeToken, null, 401);
        request("GET", "/me", secondSession, null, 401);
        request("GET", "/me", peerToken, null, 200);
        request("GET", "/me", adminToken, null, 200);
        request("POST", "/auth/login", null, fields("enterpriseDomain", domain, "loginName", "employee", "password", PASSWORD), 401);
        request("POST", "/auth/login", null, fields("enterpriseDomain", domain, "loginName", "employee", "password", "Employee-new-password"), 200);
    }

    @Test
    void employeePasswordResetEnforcesAuthorityScopeVersionAndPasswordBounds() throws Exception {
        String path = "/admin/users/" + employee + "/password-reset";
        var command = fields("expectedVersion", 0, "newPassword", "Employee-new-password");
        request("POST", path, null, command, 401);
        request("POST", path, employeeToken, command, 403);
        request("POST", path, founderToken, command, 403);
        assertEquals("ADMIN_PASSWORD_RESET_FORBIDDEN", request("POST", "/admin/users/" + admin + "/password-reset", adminToken, command, 403).get("code"));
        request("POST", path, adminToken, fields("expectedVersion", 9, "newPassword", "Employee-new-password"), 409);
        request("POST", path, adminToken, fields("newPassword", "Employee-new-password"), 422);
        request("POST", path, adminToken, fields("expectedVersion", -1, "newPassword", "Employee-new-password"), 422);
        request("POST", path, adminToken, fields("expectedVersion", 0, "newPassword", "short"), 422);
        request("POST", path, adminToken, fields("expectedVersion", 0, "newPassword", "密".repeat(25)), 422);
        request("POST", "/admin/users/" + UUID.randomUUID() + "/password-reset", adminToken, command, 404);
        var other = request("POST", "/admin/enterprise-initializations", null,
                fields("loginDomain", "reset-other-" + UUID.randomUUID(), "name", "Other", "adminLoginName", "admin", "adminPassword", PASSWORD, "adminDisplayName", "Admin"),
                201, "X-Bootstrap-Token", "integration-tests-only-bootstrap");
        String otherToken = (String) request("POST", "/auth/login", null, fields("enterpriseDomain", other.get("loginDomain"), "loginName", "admin", "password", PASSWORD), 200).get("accessToken");
        request("POST", path, otherToken, command, 404);
        assertEquals(0, number(db.entity("app_user", enterprise, employee), "version"));
        request("GET", "/me", employeeToken, null, 200);
    }

    @Test
    void concurrentEmployeePasswordResetsAllowOnlyOneCurrentVersion() throws Exception {
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { start.await(); return raw("POST", "/admin/users/" + employee + "/password-reset", adminToken, fields("expectedVersion", 0, "newPassword", "Employee-password-one")); });
            var second = executor.submit(() -> { start.await(); return raw("POST", "/admin/users/" + employee + "/password-reset", adminToken, fields("expectedVersion", 0, "newPassword", "Employee-password-two")); });
            start.countDown();
            assertEquals(List.of(200, 409), List.of(first.get(20, TimeUnit.SECONDS).status(), second.get(20, TimeUnit.SECONDS).status()).stream().sorted().toList());
        }
        assertEquals(1, number(db.entity("app_user", enterprise, employee), "version"));
    }

    @Test
    void concurrentOrganizationConversionAndManagerPromotionPreserveRoleKindInvariant() throws Exception {
        String leaf = createOrg("Concurrent leaf", "DEPARTMENT", root);
        String member = createUser("concurrent-member", leaf, founder, List.of("EMPLOYEE"));
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var conversion = executor.submit(() -> { start.await(); return raw("PATCH", "/admin/org-units/"+leaf, adminToken,
                    fields("expectedVersion", 0, "name", "Concurrent leaf", "kind", "TEAM", "parentId", department)); });
            var promotion = executor.submit(() -> { start.await(); return raw("PATCH", "/admin/users/"+member, adminToken,
                    personnelPatch(member, 0, List.of("DEPARTMENT_MANAGER"))); });
            start.countDown();
            var statuses = List.of(conversion.get(20, TimeUnit.SECONDS).status(), promotion.get(20, TimeUnit.SECONDS).status()).stream().sorted().toList();
            assertEquals(List.of(200, 422), statuses);
        }
        String kind = str(db.entity("org_unit", enterprise, leaf), "kind");
        boolean managerRole = !db.rows("SELECT role_code FROM user_role WHERE enterprise_id=? AND user_id=? AND role_code='DEPARTMENT_MANAGER'", enterprise, member).isEmpty();
        assertEquals("DEPARTMENT".equals(kind), managerRole);
    }

    @Test
    void pureAdministratorCannotProvisionCompaniesOrAccessBusinessAndEmployeesCannotCreateAccounts() throws Exception {
        var body = fields("loginDomain", "denied-"+UUID.randomUUID(), "name", "Denied", "adminLoginName", "admin", "adminPassword", PASSWORD, "adminDisplayName", "Admin");
        request("POST", "/admin/enterprise-initializations", adminToken, body, 403);
        assertTrue(db.rows("SELECT id FROM enterprise WHERE login_domain=?", body.get("loginDomain")).isEmpty());
        request("POST", "/admin/users", employeeToken, personnelCommand("denied"), 403);
        request("POST", "/admin/users", null, personnelCommand("denied"), 401);
        for (String path : List.of("/tags", "/tasks", "/work-logs", "/statistics"))
            request("GET", path, adminToken, null, 403);
        request("POST", "/tags", adminToken, fields("name", "No", "colorHex", "#123456"), 403);
        var tag = request("POST", "/tags", founderToken, fields("name", "Business", "colorHex", "#123456"), 201);
        request("PATCH", "/tags/"+tag.get("id"), adminToken, fields("expectedVersion", 0, "name", "No"), 403);
        request("DELETE", "/tags/"+tag.get("id"), adminToken, null, 403);
        assertEquals(false, ((Map<?,?>)request("GET", "/me", adminToken, null, 200).get("capabilities")).get("createTags"));
    }

    @Test
    void recipientCandidatesExposeStableDepartmentMetadataWithoutPersonnelSecrets() throws Exception {
        var candidates = items(request("GET", "/tasks/recipient-candidates?limit=1", managerToken, null, 200));
        assertEquals(1, candidates.size());
        var value = candidates.getFirst();
        assertEquals(department, value.get("departmentId"));
        assertEquals("Department", value.get("departmentName"));
        assertEquals(team, value.get("orgUnitId")); assertEquals("Team", value.get("organizationName"));
        for (String key : List.of("gender", "idNumber", "phoneNumber", "passwordHash")) assertFalse(value.containsKey(key));
        var next = request("GET", "/tasks/recipient-candidates?limit=1", managerToken, null, 200).get("nextCursor");
        assertNotNull(next);
        var second = items(request("GET", "/tasks/recipient-candidates?limit=1&cursor="+next, managerToken, null, 200)).getFirst();
        assertNotEquals(value.get("id"), second.get("id"));
        assertEquals(department, second.get("departmentId"));
    }

    @Test
    void journalBusinessDatesAllowTodayEditsAndOneTimeBackfillsOnly() throws Exception {
        for (int offset : List.of(-1, -2)) {
            var old = journal(employeeToken, today.plusDays(offset), 1);
            assertEquals(List.of(), old.get("allowedActions"));
            request("PATCH", "/work-logs/"+old.get("id"), employeeToken, fields("expectedVersion", 0), 409);
            request("DELETE", "/work-logs/"+old.get("id"), employeeToken, null, 409);
            request("POST", "/work-logs", employeeToken, fields("workDate", today.plusDays(offset), "entries", List.of(fields("clientEntryKey", "x", "content", "Again"))), 409);
        }
        for (int offset : List.of(-3, 1)) {
            var rejected = request("POST", "/work-logs", employeeToken, fields("workDate", today.plusDays(offset), "entries", List.of(fields("clientEntryKey", "x", "content", "No"))), 422);
            assertEquals("JOURNAL_DATE_OUT_OF_RANGE", rejected.get("code"));
        }
        var current = journal(employeeToken, today, 1);
        assertEquals(List.of("EDIT", "DELETE"), current.get("allowedActions"));
        assertEquals(today.toString(), current.get("businessToday"));
        request("PATCH", "/work-logs/"+current.get("id"), employeeToken, fields("expectedVersion", 0, "upsertEntries", List.of(fields("entryId", entries(current).getFirst().get("entryId"), "content", "Same ID"))), 200);
        request("DELETE", "/work-logs/"+current.get("id"), employeeToken, null, 200);
    }


    @Test
    void journalEditDateUsesCompanyTimezoneRatherThanElapsedHours() throws Exception {
        db.update("UPDATE enterprise SET business_timezone='Pacific/Kiritimati' WHERE id=?", enterprise);
        LocalDate companyToday = LocalDate.now(ZoneId.of("Pacific/Kiritimati"));
        var journal = request("POST", "/work-logs", employeeToken,
                fields("workDate", companyToday, "entries", List.of(fields("clientEntryKey", "zone", "content", "Timezone boundary"))), 201);
        assertEquals(companyToday.toString(), journal.get("businessToday"));
        db.update("UPDATE enterprise SET business_timezone='Etc/GMT+12' WHERE id=?", enterprise);
        assertTrue(LocalDate.now(ZoneId.of("Etc/GMT+12")).isBefore(companyToday));
        assertEquals(List.of(), request("GET", "/work-logs/"+journal.get("id"), employeeToken, null, 200).get("allowedActions"));
        request("PATCH", "/work-logs/"+journal.get("id"), employeeToken, fields("expectedVersion", 0), 409);
        request("DELETE", "/work-logs/"+journal.get("id"), employeeToken, null, 409);
        assertEquals(0, number(db.entity("work_log", enterprise, str(journal, "id")), "version"));
    }

    @Test
    void approvedDelegationNotifiesEveryRecipientAndReadingIsIdempotent() throws Exception {
        var task = createTask(managerToken, "DELEGATED", List.of(employee, peer));
        reviewDecision(founderToken, task, "APPROVE");
        for (String token : List.of(employeeToken, peerToken)) {
            var list = items(request("GET", "/notifications", token, null, 200));
            assertEquals(1, list.size());
            assertEquals("NEW_TASK", list.getFirst().get("kind"));
            assertEquals(str(task, "id"), list.getFirst().get("taskId"));
            assertEquals(1, unread(token));
        }
        String id = str(items(request("GET", "/notifications", employeeToken, null, 200)).getFirst(), "id");
        request("POST", "/notifications/"+id+"/read", peerToken, null, 404);
        assertNotNull(request("POST", "/notifications/"+id+"/read", employeeToken, null, 200).get("readAt"));
        assertEquals(0, unread(employeeToken));
        request("POST", "/notifications/"+id+"/read", employeeToken, null, 200);
        assertEquals(0, unread(employeeToken));
        assertEquals(1, unread(peerToken));
    }

    @Test
    void progressAndAcceptanceNotifyCreatorAndRecipientWithoutDuplicates() throws Exception {
        var task = createTask(managerToken, "DELEGATED", List.of(employee));
        reviewDecision(founderToken, task, "APPROVE");
        String taskId = str(task, "id");
        String assignmentId = assignment(taskId, employee);
        request("POST", "/task-assignments/"+assignmentId+"/progress-records", employeeToken,
                fields("expectedVersion", 1, "progressPercent", 50), 201);
        assertEquals(1, countKind(managerToken, "STATUS"));
        request("POST", "/task-assignments/"+assignmentId+"/progress-records", employeeToken,
                fields("expectedVersion", 2, "progressPercent", 50), 201);
        assertEquals(1, countKind(managerToken, "STATUS"));
        var submitted = request("POST", "/task-assignments/"+assignmentId+"/completion-requests", employeeToken,
                fields("expectedVersion", 3, "note", "done"), 201);
        assertEquals(2, countKind(managerToken, "STATUS"));
        var completion = (Map<String, Object>) submitted.get("request");
        request("POST", "/task-completion-requests/"+str(completion, "id")+"/decision", managerToken,
                fields("expectedVersion", 0, "decision", "ACCEPT"), 200);
        assertEquals(3, countKind(employeeToken, "STATUS"));
        assertEquals(3, countKind(managerToken, "STATUS"));
    }

    @Test
    void deadlineScanNotifiesPendingWorkOnlyAndNeverRepeats() throws Exception {
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        LocalTime now = LocalTime.now(zone);
        LocalDate end = LocalDate.now(zone);
        int endMinute = 23 * 60 + 59;
        // 深夜运行时把截止时刻放到次日凌晨，保证仍落在未来 24 小时窗口内
        if (now.isAfter(LocalTime.of(22, 0))) {
            end = end.plusDays(1);
            endMinute = 30;
        }
        var delegated = createTask(managerToken, "DELEGATED", List.of(employee));
        reviewDecision(founderToken, delegated, "APPROVE");
        dueAt(str(delegated, "id"), end, endMinute);
        // Approval is a visibility boundary: pending and rejected tasks must stay private.
        var pending = createTask(managerToken, "DELEGATED", List.of(peer));
        dueAt(str(pending, "id"), end, endMinute);
        var rejected = createTask(managerToken, "DELEGATED", List.of(peer));
        reviewDecision(founderToken, rejected, "REJECT");
        dueAt(str(rejected, "id"), end, endMinute);
        var personal = createTask(employeeToken, "PERSONAL", List.of());
        reviewDecision(leadToken, personal, "APPROVE");
        dueAt(str(personal, "id"), end, endMinute);
        request("POST", "/tasks/"+str(personal, "id")+"/complete", employeeToken,
                fields("expectedAssignmentVersion", 1), 200);
        assertTrue(notifications.scanDeadlines() >= 1);
        assertEquals(1, countKind(employeeToken, "DEADLINE"));
        assertEquals(0, unread(peerToken));
        assertNotNull(items(request("GET", "/notifications?limit=100", employeeToken, null, 200)).stream()
                .filter(n -> "DEADLINE".equals(n.get("kind"))).findFirst().get().get("dueAt"));
        // 已完成的任务不产生截止提醒（US29），重复扫描也不重复生成
        notifications.scanDeadlines();
        assertEquals(1, countKind(employeeToken, "DEADLINE"));
    }

    @Test
    void returningToEarlierProgressIsANewEventAndPersonalStatusIsNotDuplicated() throws Exception {
        var task = createTask(employeeToken, "PERSONAL", List.of());
        reviewDecision(leadToken, task, "APPROVE");
        String assignmentId = assignment(str(task, "id"), employee);
        int version = 1;
        for (int progress : List.of(50, 75, 50, 50)) {
            request("POST", "/task-assignments/"+assignmentId+"/progress-records", employeeToken,
                    fields("expectedVersion", version++, "progressPercent", progress), 201);
        }
        assertEquals(3, countKind(employeeToken, "STATUS"));
        assertEquals(0, countKind(peerToken, "STATUS"));
    }

    @Test
    void concurrentDuplicateNotificationsKeepOneRecordWithoutFailingCallers() throws Exception {
        var task = createTask(employeeToken, "PERSONAL", List.of());
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(8)) {
            var results = new ArrayList<Future<?>>();
            for (int i = 0; i < 8; i++) results.add(pool.submit(() -> {
                start.await();
                notifications.emit(enterprise, employee, "STATUS", str(task, "id"), null,
                        "Concurrent", "Same event", "concurrent-test", null);
                return null;
            }));
            start.countDown();
            for (var result : results) result.get(15, TimeUnit.SECONDS);
        }
        assertEquals(1, countKind(employeeToken, "STATUS"));
    }

    private void dueAt(String taskId, LocalDate end, int endMinute) {
        db.update("UPDATE task SET start_date=?,end_date=?,time_kind='TIMED',start_minute=0,end_minute=?"
                + " WHERE enterprise_id=? AND id=?", end, end, endMinute, enterprise, taskId);
    }

    private long unread(String token) throws Exception {
        return number(request("GET", "/notifications/unread-count", token, null, 200), "unreadCount");
    }

    private long countKind(String token, String kind) throws Exception {
        return items(request("GET", "/notifications?limit=100", token, null, 200)).stream()
                .filter(n -> kind.equals(n.get("kind")))
                .count();
    }

    private Map<String, Object> user(String id) throws Exception {
        return items(request("GET", "/admin/users?limit=100", adminToken, null, 200)).stream().filter(value -> id.equals(value.get("id"))).findFirst().orElseThrow();
    }

    private Map<String, Object> handoff(String id) throws Exception {
        return items(request("GET", "/admin/credential-handoffs?userId=" + id, adminToken, null, 200)).getFirst();
    }

    @Test
    void initialCredentialHandoffSurvivesNewAdminSessionAndUsesCurrentLogin() throws Exception {
        var first = handoff(employee);
        assertEquals("AVAILABLE", first.get("status"));
        assertEquals(PASSWORD, first.get("initialPassword"));
        assertNotNull(first.get("expiresAt"));
        var encrypted = db.one("SELECT * FROM credential_handoff WHERE enterprise_id=? AND user_id=?", enterprise, employee);
        assertFalse(str(encrypted, "encrypted_password").contains(PASSWORD));
        assertNotEquals(encrypted.get("encrypted_password"), db.one("SELECT encrypted_password FROM credential_handoff WHERE enterprise_id=? AND user_id=?", enterprise, peer).get("encrypted_password"));
        var person = user(employee);
        assertFalse(person.containsKey("initialPassword"));
        assertFalse(person.containsKey("encryptedPassword"));
        db.update("UPDATE app_user SET login_name='renamed-employee' WHERE enterprise_id=? AND id=?", enterprise, employee);
        String newAdmin = login("admin");
        var again = items(request("GET", "/admin/credential-handoffs", newAdmin, null, 200));
        var found = again.stream().filter(value -> employee.equals(value.get("personId"))).findFirst().orElseThrow();
        assertEquals("renamed-employee", found.get("loginName"));
        assertEquals(PASSWORD, found.get("initialPassword"));
        assertTrue(again.stream().noneMatch(value -> admin.equals(value.get("personId"))));
        var response = mvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/credential-handoffs").header("Authorization", "Bearer " + adminToken)).andReturn().getResponse();
        assertTrue(response.getHeader("Cache-Control").contains("no-store"));
    }

    @Test
    void employeeChangeErasesHandoffEvenWhenPasswordTextIsUnchanged() throws Exception {
        String second = session(employee);
        request("POST", "/me/password", employeeToken, fields("currentPassword", PASSWORD, "newPassword", PASSWORD), 204);
        assertEquals("CHANGED", handoff(employee).get("status"));
        assertFalse(handoff(employee).containsKey("initialPassword"));
        var stored = db.one("SELECT * FROM credential_handoff WHERE enterprise_id=? AND user_id=?", enterprise, employee);
        assertNull(stored.get("encrypted_password"));
        assertNull(stored.get("hash_fingerprint"));
        request("GET", "/me", employeeToken, null, 401);
        request("GET", "/me", second, null, 200);
        assertEquals(PASSWORD, handoff(peer).get("initialPassword"));
    }

    @Test
    void resetReplacesDeliverablePasswordAndExpiryErasesIt() throws Exception {
        request("POST", "/admin/users/" + employee + "/password-reset", adminToken,
                fields("expectedVersion", number(user(employee), "version"), "newPassword", "replacement-delivery-password"), 200);
        assertEquals("replacement-delivery-password", handoff(employee).get("initialPassword"));
        db.update("UPDATE credential_handoff SET expires_at=? WHERE enterprise_id=? AND user_id=?", OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1), enterprise, employee);
        assertEquals("EXPIRED", handoff(employee).get("status"));
        assertFalse(handoff(employee).containsKey("initialPassword"));
        assertNull(db.one("SELECT encrypted_password FROM credential_handoff WHERE enterprise_id=? AND user_id=?", enterprise, employee).get("encrypted_password"));
    }

    @Test
    void handoffRejectsUnauthorizedForeignAdminAndTamperedCiphertext() throws Exception {
        request("GET", "/admin/credential-handoffs", null, null, 401);
        request("GET", "/admin/credential-handoffs", employeeToken, null, 403);
        request("GET", "/admin/credential-handoffs?userId=" + admin, adminToken, null, 403);
        request("GET", "/admin/credential-handoffs?userId=" + UUID.randomUUID(), adminToken, null, 404);
        var foreign = request("POST", "/admin/enterprise-initializations", null,
                fields("loginDomain", "handoff-foreign-" + UUID.randomUUID(), "name", "Foreign", "adminLoginName", "foreign-admin", "adminPassword", PASSWORD, "adminDisplayName", "Foreign"), 201,
                "X-Bootstrap-Token", "integration-tests-only-bootstrap");
        request("GET", "/admin/credential-handoffs?userId=" + foreign.get("adminUserId"), adminToken, null, 404);
        String cipher = str(db.one("SELECT encrypted_password FROM credential_handoff WHERE enterprise_id=? AND user_id=?", enterprise, peer), "encrypted_password");
        db.update("UPDATE credential_handoff SET encrypted_password=? WHERE enterprise_id=? AND user_id=?", cipher, enterprise, employee);
        assertEquals("HANDOFF_UNAVAILABLE", request("GET", "/admin/credential-handoffs?userId=" + employee, adminToken, null, 503).get("code"));
        assertEquals(PASSWORD, handoff(peer).get("initialPassword"));
    }

    @Test
    void legacyDisabledAndExternallyChangedAccountsNeverExportGuessedCredentials() throws Exception {
        db.update("DELETE FROM credential_handoff WHERE enterprise_id=? AND user_id=?", enterprise, employee);
        assertEquals("UNAVAILABLE", handoff(employee).get("status"));
        db.update("UPDATE app_user SET password_hash='external-update' WHERE enterprise_id=? AND id=?", enterprise, peer);
        assertEquals("CHANGED", handoff(peer).get("status"));
        assertFalse(handoff(peer).containsKey("initialPassword"));
        db.update("UPDATE app_user SET status='DISABLED' WHERE enterprise_id=? AND id=?", enterprise, lead);
        assertEquals("UNAVAILABLE", handoff(lead).get("status"));
        assertFalse(handoff(lead).containsKey("initialPassword"));
    }

    @Test
    void concurrentExportCannotRestoreSecretsAfterEmployeeChange() throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var export = pool.submit(() -> { start.await(); return handoff(employee); });
            var change = pool.submit(() -> { start.await(); return request("POST", "/me/password", employeeToken, fields("currentPassword", PASSWORD, "newPassword", "self-selected-secret-123"), 204); });
            start.countDown(); export.get(20, TimeUnit.SECONDS); change.get(20, TimeUnit.SECONDS);
        }
        assertEquals("CHANGED", handoff(employee).get("status"));
        assertFalse(handoff(employee).containsKey("initialPassword"));
    }

    @Test
    void scheduledCleanupErasesExpiredAndExternallyChangedSecrets() throws Exception {
        db.update("UPDATE credential_handoff SET expires_at=? WHERE enterprise_id=? AND user_id=?", OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1), enterprise, employee);
        db.update("UPDATE app_user SET password_hash='external-update' WHERE enterprise_id=? AND id=?", enterprise, peer);
        credentialHandoffs.expire();
        assertEquals("EXPIRED", handoff(employee).get("status"));
        assertEquals("CHANGED", handoff(peer).get("status"));
        assertNull(db.one("SELECT encrypted_password FROM credential_handoff WHERE enterprise_id=? AND user_id=?", enterprise, employee).get("encrypted_password"));
        assertNull(db.one("SELECT encrypted_password FROM credential_handoff WHERE enterprise_id=? AND user_id=?", enterprise, peer).get("encrypted_password"));
    }

}
