package com.company.ai.marketplace.security;

/**
 * 当前请求上下文 ThreadLocal：保存 LoginUser。
 * 在 AuthInterceptor 写入，afterCompletion 清除。
 */
public class ThreadLocalContext {
    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) { HOLDER.set(user); }
    public static LoginUser get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }
}
