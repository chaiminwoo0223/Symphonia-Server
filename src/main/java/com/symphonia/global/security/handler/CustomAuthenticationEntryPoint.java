package com.symphonia.global.security.handler;

import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.auth.domain.exception.TokenStoreUnavailableException;
import com.symphonia.global.security.filter.JwtAuthenticationFilter;
import com.symphonia.global.security.writer.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private static final String BEARER_CHALLENGE = "Bearer realm=\"symphonia\"";

    private final ErrorResponseWriter errorResponseWriter;

    @Override
    public void commence(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull AuthenticationException authException)
            throws IOException {
        if (request.getAttribute(JwtAuthenticationFilter.TOKEN_STORE_UNAVAILABLE_ATTRIBUTE)
                instanceof TokenStoreUnavailableException exception) {
            errorResponseWriter.send(response, exception.getErrorCode());
            return;
        }

        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, BEARER_CHALLENGE);
        errorResponseWriter.send(response, AuthErrorCode.UNAUTHENTICATED);
    }
}
