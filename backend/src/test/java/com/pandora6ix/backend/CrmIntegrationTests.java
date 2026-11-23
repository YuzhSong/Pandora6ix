package com.pandora6ix.backend;

import static com.pandora6ix.backend.repository.BusinessDb.*;
import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.pandora6ix.backend.repository.BusinessDb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pandora-crm-tests;DB_CLOSE_DELAY=-1",
        "spring.flyway.locations=classpath:db/migration,classpath:db/crm"
})
@ActiveProfiles("dev2")
@AutoConfigureMockMvc
class CrmIntegrationTests {
    @Autowired BusinessDb db;
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;

    private static final String PASSWORD = "crm-fixture-only-123";
    private static final String PASSWORD_HASH = new BCryptPasswordEncoder().encode(PASSWORD);
    private String enterprise;
    private User admin;
    private User employee;
    private User peer;
    private User outsider;

    private record User(String id, String token) {}

    @BeforeEach
    void setup() throws Exception {
        enterprise = enterprise();
        admin = account(enterprise, "ADMIN");
        employee = account(enterprise, "EMPLOYEE");
        peer = account(enterprise, "EMPLOYEE");
        outsider = account(enterprise(), "ADMIN");
    }

    @Test
    void customerOwnershipAndVersionAreEnforced() throws Exception {
        String customer = customer(employee);
        assertEquals("fictional@example.test", request("GET", "/crm/customers/" + customer,
                employee, null, 200).get("contact").asString());
        request("GET", "/crm/customers/" + customer, peer, null, 404);
        request("GET", "/crm/customers/" + customer, outsider, null, 404);
        request("GET", "/crm/customers/" + customer, null, null, 401);
        assertEquals(0, request("GET", "/crm/customers", peer, null, 200).size());
        assertEquals(1, request("GET", "/crm/customers?kind=COMPANY&q=fiction", admin, null, 200).size());
        assertEquals(0, request("GET", "/crm/customers?kind=PERSON", admin, null, 200).size());

        request("POST", "/crm/customers", admin, customerInput(outsider), 422);
        request("POST", "/crm/customers", employee, customerInput(employee), 403);
        var edit = customerInput(peer);
        edit.put("version", 0);
        request("PUT", "/crm/customers/" + customer, admin, edit, 200);
        request("PUT", "/crm/customers/" + customer, admin, edit, 409);
        request("GET", "/crm/customers/" + customer, employee, null, 404);
        request("GET", "/crm/customers/" + customer, peer, null, 200);
        assertEquals(2, db.rows("SELECT id FROM operation_log WHERE object_id=? AND module='CRM'", customer).size());
        assertTrue(request("GET", "/me", admin, null, 200).isObject());
    }

    private String customer(User owner) throws Exception {
        return request("POST", "/crm/customers", admin, customerInput(owner), 201).get("id").asString();
    }

    private Map<String, Object> customerInput(User owner) {
        return fields("name", "Fictional customer", "kind", "COMPANY",
                "contact", "fictional@example.test", "ownerUserId", owner.id());
    }

    private String enterprise() {
        String id = id();
        db.update("INSERT INTO enterprise(id,login_domain,name,business_timezone) VALUES (?,?,?,?)",
                id, id, "Fictional enterprise", "Asia/Shanghai");
        return id;
    }

    private User account(String enterpriseId, String role) throws Exception {
        String id = id();
        db.update("""
                INSERT INTO app_user(id,enterprise_id,login_name,password_hash,display_name)
                VALUES(?,?,?,?,?)
                """, id, enterpriseId, id, PASSWORD_HASH, "Fictional " + role);
        db.update("INSERT INTO user_role(enterprise_id,user_id,role_code) VALUES(?,?,?)",
                enterpriseId, id, role);
        var token = request("POST", "/auth/login", null,
                fields("enterpriseDomain", enterpriseId, "loginName", id, "password", PASSWORD),
                200).get("accessToken").asString();
        return new User(id, token);
    }

    private JsonNode request(String method, String path, User user, Object body, int status) throws Exception {
        var req = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                HttpMethod.valueOf(method), "/api/v1" + path);
        if (user != null) req.header("Authorization", "Bearer " + user.token());
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        var payload = json.readTree(mvc.perform(req).andExpect(status().is(status))
                .andReturn().getResponse().getContentAsString());
        return status < 300 ? payload.get("data") : payload;
    }
}
