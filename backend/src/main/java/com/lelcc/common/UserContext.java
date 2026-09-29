package com.lelcc.common;

public class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getId() {
        LoginUser loginUser = HOLDER.get();
        return loginUser == null ? null : loginUser.id();
    }

    public static UserType getType() {
        LoginUser loginUser = HOLDER.get();
        return loginUser == null ? null : loginUser.type();
    }

    public static void remove() {
        HOLDER.remove();
    }
}
