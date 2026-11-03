package com.pandora6ix.backend.service.ai;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 每人每个业务日的模型调用上限；单实例部署，内存计数，跨日自动失效 */
@Component
public class AiQuota {
    private final int dailyLimit;
    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    public AiQuota(@Value("${pandora.ai.daily-quota:30}") int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public boolean tryAcquire(String userId, LocalDate businessDate) {
        String suffix = "|" + businessDate;
        if (counters.size() > 10_000) counters.keySet().removeIf(key -> !key.endsWith(suffix));
        return counters.computeIfAbsent(userId + suffix, key -> new AtomicInteger()).incrementAndGet() <= dailyLimit;
    }
}
