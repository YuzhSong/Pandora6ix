package com.pandora6ix.backend.service.ai;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** 发送给第三方前的脱敏：成员姓名/登录名 → “同事”；网址、邮箱、手机号与长数字串 → [已隐藏] */
final class AiRedactor {
    static final String HIDDEN = "[已隐藏]";
    private static final Pattern URL = Pattern.compile("(?i)\\b(?:https?://|www\\.)\\S+");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+");
    private static final Pattern DIGITS = Pattern.compile("(?<!\\d)\\+?\\d[\\d -]{4,}\\d(?!\\d)");
    private final List<String> names;

    AiRedactor(Collection<String> names) {
        this.names = names.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> name.codePointCount(0, name.length()) >= 2)
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
    }

    String redact(String text) {
        if (text == null) return "";
        String result = URL.matcher(text).replaceAll(HIDDEN);
        result = EMAIL.matcher(result).replaceAll(HIDDEN);
        for (String name : names) result = result.replace(name, "同事");
        return DIGITS.matcher(result).replaceAll(match ->
                match.group().replaceAll("\\D", "").length() >= 6 ? HIDDEN : match.group());
    }

    String excerpt(String text, int max) {
        return AiOutput.truncate(redact(text), max);
    }
}
