package com.healthapp.security;

import com.healthapp.common.BusinessException;

public class UserContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private UserContext() {}

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        Long userId = USER_ID.get();
        if (userId == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return userId;
    }

    public static void clear() {
        USER_ID.remove();
    }
}
