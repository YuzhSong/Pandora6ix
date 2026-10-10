package com.pandora6ix.backend.api.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pandora6ix.backend.Pandora6ixBackendApplication;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = Pandora6ixBackendApplication.class)
@AutoConfigureMockMvc
@Import(ApiErrorContractTests.ErrorProbeController.class)
class ApiErrorContractTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void unknownRouteReturnsUniformJsonError() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(header().exists(RequestIdFilter.HEADER_NAME))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void callerRequestIdIsPropagated() throws Exception {
        mockMvc.perform(get("/health").header(RequestIdFilter.HEADER_NAME, "test-request-1"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "test-request-1"));
    }

    @Test
    void unsupportedMethodRetainsClientErrorAndAllowHeader() throws Exception {
        mockMvc.perform(post("/health").header(RequestIdFilter.HEADER_NAME, "method-test"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.path").value("/health"))
                .andExpect(jsonPath("$.requestId").value("method-test"));
    }

    @Test
    void validationFailureReturnsUniformBadRequest() throws Exception {
        mockMvc.perform(post("/__test/error-probe").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void malformedJsonReturnsUniformBadRequest() throws Exception {
        mockMvc.perform(post("/__test/error-probe").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void unexpectedFailureDoesNotExposeInternalMessage() throws Exception {
        mockMvc.perform(get("/__test/error-probe"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred."));
    }

    @RestController
    static class ErrorProbeController {
        record Input(@NotBlank String name) {}

        @PostMapping("/__test/error-probe")
        String validate(@Valid @RequestBody Input input) {
            return input.name();
        }

        @GetMapping("/__test/error-probe")
        String fail() {
            throw new IllegalStateException("Internal detail that must not be exposed");
        }
    }
}
