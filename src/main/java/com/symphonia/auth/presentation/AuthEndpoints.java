package com.symphonia.auth.presentation;

public final class AuthEndpoints {
    private AuthEndpoints() {}

    private static final String BASE = "/api/v1/auth";

    public static final String SIGNUP = BASE + "/signup";
    public static final String LOGIN = BASE + "/login";
    public static final String REFRESH = BASE + "/refresh";
    public static final String LOGOUT = BASE + "/logout";
}
