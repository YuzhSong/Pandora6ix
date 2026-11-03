package com.pandora6ix.backend.service.ai;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/** 统一调用流程：未配置/超额直接兜底；单次超时 + 至多重试一次，总预算 40 秒；日志不含内容 */
@Component
public class AiEngine {
    private static final Logger log = LoggerFactory.getLogger(AiEngine.class);
    private static final Duration BUDGET = Duration.ofSeconds(40);
    private static final Duration MIN_ATTEMPT = Duration.ofSeconds(5);
    private final DeepSeekGateway gateway;
    private final AiPrompts prompts;
    private final AiQuota quota;
    private final Duration attemptTimeout;

    public AiEngine(DeepSeekGateway gateway, AiPrompts prompts, AiQuota quota,
            @Value("${pandora.deepseek.timeout-seconds:25}") long timeoutSeconds) {
        this.gateway = gateway;
        this.prompts = prompts;
        this.quota = quota;
        this.attemptTimeout = Duration.ofSeconds(Math.max(5, timeoutSeconds));
    }

    @FunctionalInterface
    interface Parser {
        Map<String, Object> parse(JsonNode output) throws AiOutput.InvalidOutputException;
    }

    /** fields 为 null 表示使用兜底内容 */
    record Generation(Map<String, Object> fields, boolean quotaExceeded) {
        boolean generated() { return fields != null; }
    }

    Generation generate(String userId, LocalDate businessDate, String feature, Object input,
            double temperature, int maxTokens, Parser parser) {
        if (!gateway.configured()) return new Generation(null, false);
        if (!quota.tryAcquire(userId, businessDate)) return new Generation(null, true);
        String system = prompts.system(feature), user = prompts.user(input);
        long started = System.nanoTime();
        for (int attempt = 1; attempt <= 2; attempt++) {
            Duration remaining = BUDGET.minusNanos(System.nanoTime() - started);
            if (remaining.compareTo(MIN_ATTEMPT) < 0) break;
            Duration timeout = remaining.compareTo(attemptTimeout) < 0 ? remaining : attemptTimeout;
            try {
                var fields = parser.parse(gateway.completeJson(system, user, temperature, maxTokens, timeout));
                log.info("ai_generation feature={} attempt={} durationMs={} result=ok", feature, attempt, elapsed(started));
                return new Generation(fields, false);
            } catch (DeepSeekGateway.CompletionRejectedException e) {
                log.warn("ai_generation feature={} attempt={} durationMs={} result=http_{}xx", feature, attempt, elapsed(started), e.status() / 100);
            } catch (AiOutput.InvalidOutputException e) {
                log.warn("ai_generation feature={} attempt={} durationMs={} result=invalid_output", feature, attempt, elapsed(started));
            } catch (IOException e) {
                log.warn("ai_generation feature={} attempt={} durationMs={} result={}", feature, attempt, elapsed(started), e.getClass().getSimpleName());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return new Generation(null, false);
    }

    private static long elapsed(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }
}
