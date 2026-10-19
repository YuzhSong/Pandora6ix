package com.pandora6ix.backend;

import static com.pandora6ix.backend.repository.BusinessDb.*;
import static org.junit.jupiter.api.Assertions.*;

import com.pandora6ix.backend.repository.BusinessDb;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "pandora.handoff-key=")
@AutoConfigureMockMvc
class CredentialHandoffConfigurationTests {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired BusinessDb db;

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String path, String token, Object body, int status) throws Exception {
        var request = MockMvcRequestBuilders.post("/api/v1" + path).contentType(MediaType.APPLICATION_JSON)
                .header("X-Bootstrap-Token", "integration-tests-only-bootstrap").content(json.writeValueAsString(body));
        if (token != null) request.header("Authorization", "Bearer " + token);
        var response = mvc.perform(request).andReturn().getResponse();
        assertEquals(status, response.getStatus(), response.getContentAsString());
        var parsed = json.readValue(response.getContentAsString(), Map.class);
        return (Map<String, Object>) parsed.get(status < 400 ? "data" : "error");
    }

    @Test
    void missingExternalKeyRollsBackCreationAndPasswordReset() throws Exception {
        String domain = "key-missing-" + UUID.randomUUID(), password = "fixture-password-123";
        var initialized = post("/admin/enterprise-initializations", null,
                fields("loginDomain", domain, "name", "Key test", "adminLoginName", "admin", "adminPassword", password, "adminDisplayName", "Administrator"), 201);
        String enterprise = str(initialized, "id");
        String token = str(post("/auth/login", null, fields("enterpriseDomain", domain, "loginName", "admin", "password", password), 200), "accessToken");
        var error = post("/admin/users", token, fields("loginName", "newemployee", "password", password, "displayName", "Employee", "orgUnitId", initialized.get("rootOrgUnitId"), "roles", List.of("EMPLOYEE"), "gender", "MALE", "idNumber", "000000200001010001", "phoneNumber", "00000000001"), 503);
        assertEquals("HANDOFF_KEY_NOT_CONFIGURED", error.get("code"));
        assertEquals(1, db.rows("SELECT id FROM app_user WHERE enterprise_id=?", enterprise).size());
        String user = id();
        db.update("INSERT INTO app_user(id,enterprise_id,login_name,password_hash,display_name) SELECT ?,enterprise_id,'legacy',password_hash,'Legacy' FROM app_user WHERE enterprise_id=? AND id=?", user, enterprise, initialized.get("adminUserId"));
        db.update("INSERT INTO user_role VALUES(?,?,'EMPLOYEE')", enterprise, user);
        String oldHash = str(db.entity("app_user", enterprise, user), "password_hash");
        post("/admin/users/" + user + "/password-reset", token, fields("expectedVersion", 0, "newPassword", "replacement-fixture-123"), 503);
        var unchanged = db.entity("app_user", enterprise, user);
        assertEquals(oldHash, unchanged.get("password_hash"));
        assertEquals(0, number(unchanged, "version"));
        assertTrue(db.rows("SELECT * FROM credential_handoff WHERE enterprise_id=?", enterprise).isEmpty());
    }
}
