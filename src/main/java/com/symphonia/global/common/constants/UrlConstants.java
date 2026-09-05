package com.symphonia.global.common.constants;

import java.util.List;

public final class UrlConstants {
    private UrlConstants() {}

    public static final String[] SWAGGER_PATHS = {
        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
    };

    public static final String[] PERMIT_ALL_PATHS = {
        "/api/v1/auth/login", "/api/v1/auth/refresh",
    };

    public static final List<String> CORS_ALLOWED_ORIGINS =
            List.of("http://localhost:3000", "http://localhost:5173");
}
