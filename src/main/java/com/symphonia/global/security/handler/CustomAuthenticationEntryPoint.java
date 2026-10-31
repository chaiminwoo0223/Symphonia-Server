package com.symphonia.global.security.handler;

import com.symphonia.auth.domain.error.AuthErrorCode;
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
    private static final String BEARER_CHALLENGE = "Bearer";

    private final ErrorResponseWriter errorResponseWriter;

    @Override
    public void commence(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull AuthenticationException authException)
            throws IOException {
        // 401 응답은 클라이언트가 어떤 방식으로 인증해야 하는지 알려 줘야 한다(RFC 9110, RFC 6750).
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, BEARER_CHALLENGE);
        errorResponseWriter.send(response, AuthErrorCode.UNAUTHENTICATED);
    }
}
