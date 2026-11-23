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

    @Test
    void assignedEmployeeCanReadBusinessWithoutAccessToCustomerContact() throws Exception {
        String customer = customer(admin);
        var input = fields("title", "Fictional salon", "kind", "SALON", "status", "PLANNED",
                "eventDate", "2026-12-01", "eventTime", "15:00", "customerId", customer,
                "ownerUserId", admin.id(), "participants",
                java.util.List.of(fields("userId", employee.id(), "responsibility", "Reception")));
        String business = request("POST", "/crm/businesses", admin, input, 201).get("id").asString();
        assertEquals(1, request("GET", "/crm/businesses?mine=true", employee, null, 200).size());
        assertEquals(1, request("GET", "/crm/businesses?customerId=" + customer, admin, null, 200).size());
        var detail = request("GET", "/crm/businesses/" + business, employee, null, 200);
        assertEquals("Reception", detail.get("participants").get(0).get("responsibility").asString());
        assertFalse(detail.has("contact"));
        request("GET", "/crm/customers/" + customer, employee, null, 404);
        request("GET", "/crm/businesses/" + business, peer, null, 404);
        request("GET", "/crm/businesses/" + business, outsider, null, 404);
        input.put("version", 0);
        input.put("participants", java.util.List.of(fields("userId", outsider.id(), "responsibility", "Reception")));
        request("PUT", "/crm/businesses/" + business, admin, input, 422);
        request("GET", "/crm/businesses/" + business, employee, null, 200);
        input.put("participants", java.util.List.of());
        request("PUT", "/crm/businesses/" + business, admin, input, 200);
        request("GET", "/crm/businesses/" + business, employee, null, 404);
        request("PUT", "/crm/businesses/" + business, admin, input, 409);
    }


    @Test
    void membershipEntitlementsInheritCustomerScope() throws Exception {
        String customer = customer(employee);
        var input = fields("customerId", customer, "membershipType", "Fictional annual card",
                "startDate", "2026-11-01", "endDate", "2027-11-01");
        String member = request("POST", "/crm/memberships", admin, input, 201).get("id").asString();
        String benefits = "/crm/memberships/" + member + "/benefits";
        String benefit = request("POST", benefits, admin, fields("name", "Fictional salon pass"), 201).get("id").asString();
        String uses = "/crm/benefits/" + benefit + "/uses";
        var use = fields("serviceDate", "2026-12-01", "status", "SCHEDULED");
        String usage = request("POST", uses, admin, use, 201).get("id").asString();
        use.put("status", "COMPLETED");
        use.put("version", 0);
        request("PUT", uses + "/" + usage, admin, use, 200);
        assertEquals("COMPLETED", request("GET", uses, employee, null, 200).get(0).get("status").asString());
        request("GET", uses, peer, null, 404);
        request("GET", benefits, outsider, null, 404);
        request("POST", benefits, employee, fields("name", "Forbidden"), 403);
        request("POST", uses, outsider, use, 404);
    }

    @Test
    void contractsAndAttachmentsAreRestrictedToEmployeeAndCompanyAdmin() throws Exception {
        var today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"));
        String route = "/crm/contracts";
        var input = fields("employeeUserId", employee.id(), "title", "Fictional employment",
                "startDate", today.minusMonths(2), "endDate", today.plusDays(30), "notes", "Private notes");
        var contract = request("POST", route, admin, input, 201);
        String path = route + "/" + contract.get("id").asString();
        assertEquals("EXPIRING", contract.get("expiry").asString());
        assertEquals(1, request("GET", route + "?expiry=EXPIRING", admin, null, 200).size());
        request("GET", path, employee, null, 200);
        request("GET", path, peer, null, 404);
        request("GET", path, outsider, null, 404);
        request("POST", route, peer, input, 403);
        request("POST", route, outsider, input, 404);
        String attachment = "/api/v1" + path + "/attachment";
        byte[] pdf = "%PDF-1.4\n%%EOF\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        var file = new org.springframework.mock.web.MockMultipartFile("file", "fictional.pdf", "application/pdf", pdf);
        var upload = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(HttpMethod.PUT, attachment);
        mvc.perform(upload.file(file).param("version", "0").header("Authorization", "Bearer " + employee.token()))
                .andExpect(status().isForbidden());
        var invalid = new org.springframework.mock.web.MockMultipartFile("file", "invalid.pdf", "application/pdf", new byte[10]);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(HttpMethod.PUT, attachment)
                .file(invalid).param("version", "0").header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(HttpMethod.PUT, attachment)
                .file(file).param("version", "0").header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.attachmentName").value("fictional.pdf"))
                .andExpect(jsonPath("$.data.attachmentId").doesNotExist());
        String first = str(db.entity("crm_employment_contract", enterprise, contract.get("id").asString()), "attachment_id");
        var testStorage = com.pandora6ix.backend.config.Dev2Isolation.dataRoot().resolve("test-attachments");
        var get = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(attachment);
        mvc.perform(get.header("Authorization", "Bearer " + employee.token())).andExpect(status().isOk())
                .andExpect(content().bytes(pdf)).andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
        for (User unauthorized : java.util.List.of(peer, outsider)) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(attachment)
                    .header("Authorization", "Bearer " + unauthorized.token())).andExpect(status().isNotFound());
        }
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(HttpMethod.PUT, attachment)
                .file(file).param("version", "0").header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isConflict());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(HttpMethod.PUT, attachment)
                .file(file).param("version", "1").header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk());
        assertFalse(java.nio.file.Files.exists(testStorage.resolve(first + ".pdf")));
        String latest = str(db.entity("crm_employment_contract", enterprise, contract.get("id").asString()), "attachment_id");
        java.nio.file.Files.delete(testStorage.resolve(latest + ".pdf"));
        assertTrue(db.rows("SELECT after_values FROM operation_log WHERE object_id=?",
                contract.get("id").asString()).stream().noneMatch(row -> str(row, "after_values").contains("Private")));
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
