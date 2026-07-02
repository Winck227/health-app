package com.healthapp.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateTimeUtil {
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE_TIME_MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DateTimeUtil() {}

    // 兼容秒级、分钟级和纯日期输入，避免不同调用方传来的时间格式不一致。
    public static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }
        String text = value.trim().replace('T', ' ');
        try {
            return LocalDateTime.parse(text, DATE_TIME);
        } catch (DateTimeParseException ignored) {}
        try {
            return LocalDateTime.parse(text, DATE_TIME_MINUTE);
        } catch (DateTimeParseException ignored) {}
        try {
            return LocalDate.parse(text.substring(0, 10), DATE).atStartOfDay();
        } catch (Exception ignored) {}
        return LocalDateTime.now();
    }

    // 空时间统一返回空串，前端展示时不用额外判空。
    public static String format(LocalDateTime time) {
        return time == null ? "" : time.format(DATE_TIME);
    }

    // 仅输出日期部分，供计划、资料和打卡场景共用。
    public static String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DATE);
    }
}
