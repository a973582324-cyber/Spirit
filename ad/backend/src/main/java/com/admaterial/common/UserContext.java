package com.admaterial.common;

/**
 * 线程级用户上下文
 * —— 过滤器里 SET，Controller/Service 里 GET，请求结束后 REMOVE
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getUserId();
    }

    /**
     * 确保已登录；未登录抛 401
     * —— 所有业务 Service 方法入口第一行都应调用
     */
    public static LoginUser requireLogin() {
        LoginUser u = HOLDER.get();
        if (u == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }
        return u;
    }

    public static void remove() {
        HOLDER.remove();
    }
}
