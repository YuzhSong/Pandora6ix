package com.pandora6ix.backend.domain.task;

import com.pandora6ix.backend.api.BusinessException;
import com.pandora6ix.backend.api.Contracts.Recurrence;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class RecurrenceDates {
    private RecurrenceDates() {}
    public static List<LocalDate> generate(LocalDate start, LocalDate end, Recurrence rule) {
        if (start == null || end == null || start.getYear() < 1 || end.getYear() > 9999
                || end.isBefore(start) || end.isAfter(start.plusYears(10)))
            throw BusinessException.invalid("重复范围应为开始日期起十年以内，结束不得早于开始。");
        if (rule == null || !Set.of("DAILY", "WEEKLY", "MONTHLY").contains(String.valueOf(rule.frequency()))
                || rule.interval() == null || rule.interval() < 1 || rule.interval() > 365)
            throw BusinessException.invalid("重复频率无效，间隔必须为 1–365。");
        var weekdays = rule.weekdays() == null ? List.<Integer>of() : rule.weekdays();
        if (weekdays.stream().anyMatch(n -> n == null || n < 1 || n > 7)
                || (rule.frequency().equals("WEEKLY") && weekdays.isEmpty())
                || (!rule.frequency().equals("WEEKLY") && !weekdays.isEmpty()))
            throw BusinessException.invalid("每周重复须选择星期，其他规则不填写星期。");
        List<LocalDate> dates = new ArrayList<>();
        LocalDate monday = start.minusDays(start.getDayOfWeek().getValue() - 1);
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            boolean matches = switch (rule.frequency()) {
                case "DAILY" -> ChronoUnit.DAYS.between(start, d) % rule.interval() == 0;
                case "WEEKLY" -> ChronoUnit.DAYS.between(monday, d) / 7 % rule.interval() == 0
                        && weekdays.contains(d.getDayOfWeek().getValue());
                default -> d.getDayOfMonth() == start.getDayOfMonth()
                        && ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(d)) % rule.interval() == 0;
            };
            if (matches) dates.add(d);
            if (dates.size() > 366) throw BusinessException.invalid("一次最多生成 366 次任务，请缩短范围。");
        }
        if (dates.isEmpty()) throw BusinessException.invalid("所选范围没有符合规则的日期。");
        return List.copyOf(dates);
    }

    public static String label(LocalDate start, LocalDate end, Recurrence rule) {
        String unit = switch (rule.frequency()) { case "DAILY" -> "天"; case "WEEKLY" -> "周"; default -> "个月"; };
        String detail = rule.frequency().equals("WEEKLY") ? "，星期 " + new TreeSet<>(rule.weekdays())
                : rule.frequency().equals("MONTHLY") ? "，每月 " + start.getDayOfMonth() + " 日（无该日则跳过）" : "";
        return "每 " + rule.interval() + " " + unit + detail + "；" + start + " 至 " + end;
    }
}
