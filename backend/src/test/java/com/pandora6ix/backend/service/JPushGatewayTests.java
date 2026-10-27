package com.pandora6ix.backend.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class JPushGatewayTests {
    @Test void sendsAuthenticatedRecipientOnlyPayloadWithoutBusinessBody() throws Exception {
        var authorization = new AtomicReference<String>();
        var body = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/push", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, -1); exchange.close();
        });
        server.start();
        try {
            var json = JsonMapper.builder().build();
            var gateway = new JPushGateway("fixture-key", "fixture-secret", "http://127.0.0.1:" + server.getAddress().getPort() + "/push", json);
            assertEquals(200, gateway.send("device-registration", "notification-id", "STATUS", "REVIEW_RESULT", "审批结果"));
            assertEquals("Basic " + Base64.getEncoder().encodeToString("fixture-key:fixture-secret".getBytes(StandardCharsets.UTF_8)), authorization.get());
            var payload = json.readTree(body.get());
            assertEquals("device-registration", payload.path("audience").path("registration_id").get(0).asString());
            assertEquals("notification-id", payload.path("notification").path("android").path("extras").path("notificationId").asString());
            assertFalse(body.get().contains("fixture-secret")); assertFalse(body.get().contains("Bearer"));
        } finally { server.stop(0); }
    }
    @Test void rejectedResponseIsReportedWithoutEchoingCredentials() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/push", exchange -> { exchange.getRequestBody().readAllBytes(); exchange.sendResponseHeaders(503, -1); exchange.close(); });
        server.start();
        try {
            var gateway = new JPushGateway("fixture-key", "fixture-secret", "http://127.0.0.1:" + server.getAddress().getPort() + "/push", JsonMapper.builder().build());
            var failure = assertThrows(JPushGateway.PushRejectedException.class, () -> gateway.send("registration", "notification", "STATUS", "REVIEW_RESULT", "审批结果"));
            assertEquals(503, failure.status()); assertFalse(failure.getMessage().contains("fixture-secret"));
        } finally { server.stop(0); }
    }
    @Test void missingConfigurationFailsBeforeExternalRequest() {
        var gateway = new JPushGateway("", "", "http://127.0.0.1:1", JsonMapper.builder().build());
        assertFalse(gateway.configured());
        assertThrows(IllegalStateException.class, () -> gateway.send("r", "n", "STATUS", "REVIEW_RESULT", "title"));
    }
}
