package com.pandora6ix.backend.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class JPushGateway {
    private final String appKey;
    private final String masterSecret;
    private final URI endpoint;
    private final JsonMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public JPushGateway(
            @Value("${pandora.jpush.app-key:}") String appKey,
            @Value("${pandora.jpush.master-secret:}") String masterSecret,
            @Value("${pandora.jpush.api-url:https://api.jpush.cn/v3/push}") String endpoint,
            JsonMapper json) {
        this.appKey = appKey;
        this.masterSecret = masterSecret;
        this.endpoint = URI.create(endpoint);
        this.json = json;
    }

    public boolean configured() {
        return !appKey.isBlank() && !masterSecret.isBlank();
    }

    public int send(String registrationId, String notificationId, String kind, String eventType, String title) throws IOException, InterruptedException {
        if (!configured()) throw new IllegalStateException("JPush is not configured");
        String alert = title == null || title.isBlank() ? "你有一条新消息" : title;
        Map<String, Object> android = Map.of(
                "alert", alert,
                "title", "Pandora",
                "extras", Map.of("notificationId", notificationId, "kind", kind, "eventType", eventType));
        Map<String, Object> payload = Map.of(
                "platform", List.of("android"),
                "audience", Map.of("registration_id", List.of(registrationId)),
                "notification", Map.of("android", android),
                "options", Map.of("time_to_live", 86400));
        String credential = Base64.getEncoder().encodeToString(
                (appKey + ":" + masterSecret).getBytes(StandardCharsets.UTF_8));
        var request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Basic " + credential)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload)))
                .build();
        HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new PushRejectedException(response.statusCode());
        return response.statusCode();
    }

    public static final class PushRejectedException extends IOException {
        private final int status;
        public PushRejectedException(int status) { super("JPush request failed"); this.status = status; }
        public int status() { return status; }
    }
}
