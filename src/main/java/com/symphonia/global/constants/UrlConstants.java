package com.symphonia.global.constants;

import java.util.List;

public final class UrlConstants {
    private UrlConstants() {}

    public static final String[] SWAGGER_PATHS = {
        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
    };

    public static final String SIGNUP_PATH = "/api/v1/auth/signup";
    public static final String LOGIN_PATH = "/api/v1/auth/login";
    public static final String REFRESH_PATH = "/api/v1/auth/refresh";

    public static final String[] PERMIT_ALL_PATHS = {SIGNUP_PATH, LOGIN_PATH, REFRESH_PATH};

    public static final List<String> CORS_ALLOWED_ORIGINS =
            List.of("http://localhost:3000", "http://localhost:5173");
}
