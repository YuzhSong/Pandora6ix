package com.pandora6ix.backend.service.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** DeepSeek Chat Completions（OpenAI 兼容）；密钥只来自服务器环境变量，不进入日志或响应 */
@Component
public class DeepSeekGateway {
    private final String apiKey;
    private final URI endpoint;
    private final String model;
    private final JsonMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public DeepSeekGateway(
            @Value("${pandora.deepseek.api-key:}") String apiKey,
            @Value("${pandora.deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${pandora.deepseek.model:deepseek-chat}") String model,
            JsonMapper json) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.endpoint = URI.create(baseUrl.replaceAll("/+$", "") + "/chat/completions");
        this.model = model;
        this.json = json;
    }

    public boolean configured() {
        return !apiKey.isBlank();
    }

    /** 以 JSON 模式请求一次补全，返回模型输出解析后的 JSON 对象 */
    public JsonNode completeJson(String system, String user, double temperature, int maxTokens, Duration timeout)
            throws IOException, InterruptedException {
        if (!configured()) throw new IllegalStateException("DeepSeek is not configured");
        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "system", "content", system), Map.of("role", "user", "content", user)),
                "response_format", Map.of("type", "json_object"),
                "temperature", temperature,
                "max_tokens", maxTokens,
                "stream", false);
        var request = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new CompletionRejectedException(response.statusCode());
        try {
            String content = json.readTree(response.body()).path("choices").path(0).path("message").path("content").asString("");
            JsonNode output = json.readTree(content.isBlank() ? "{}" : content);
            if (!output.isObject() || output.isEmpty()) throw new AiOutput.InvalidOutputException("empty output");
            return output;
        } catch (JacksonException e) {
            throw new AiOutput.InvalidOutputException("output is not JSON");
        }
    }

    public static final class CompletionRejectedException extends IOException {
        private final int status;
        public CompletionRejectedException(int status) { super("DeepSeek request failed"); this.status = status; }
        public int status() { return status; }
    }
}
