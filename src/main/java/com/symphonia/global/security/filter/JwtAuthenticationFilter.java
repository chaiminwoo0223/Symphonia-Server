package com.symphonia.global.security.filter;

import com.symphonia.auth.domain.exception.TokenStoreUnavailableException;
import com.symphonia.auth.domain.provider.AccessTokenProvider;
import com.symphonia.auth.domain.repository.BlacklistAccessTokenRepository;
import com.symphonia.auth.domain.repository.WithdrawnMemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String TOKEN_STORE_UNAVAILABLE_ATTRIBUTE =
            JwtAuthenticationFilter.class.getName() + ".TOKEN_STORE_UNAVAILABLE";

    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenProvider accessTokenProvider;
    private final BlacklistAccessTokenRepository blacklistAccessTokenRepository;
    private final WithdrawnMemberRepository withdrawnMemberRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String accessToken = extractBearerToken(request);

        try {
            authenticateIfValid(accessToken);
        } catch (TokenStoreUnavailableException e) {
            log.warn(
                    "토큰 저장소를 조회하지 못해 인증하지 않고 요청을 넘깁니다. 원인: {}",
                    e.getCause().getClass().getSimpleName());
            request.setAttribute(TOKEN_STORE_UNAVAILABLE_ATTRIBUTE, e);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateIfValid(String accessToken) {
        if (StringUtils.hasText(accessToken) && isValidToken(accessToken)) {
            setAuthentication(accessToken);
        }
    }

    private boolean isValidToken(String accessToken) {
        return accessTokenProvider.validate(accessToken)
                && !blacklistAccessTokenRepository.isBlacklisted(accessToken)
                && !withdrawnMemberRepository.isWithdrawn(
                        accessTokenProvider.getMemberId(accessToken));
    }

    private void setAuthentication(String accessToken) {
        String memberId = accessTokenProvider.getMemberId(accessToken);
        String role = accessTokenProvider.getRole(accessToken);

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(memberId, accessToken, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!(StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX))) {
            return null;
        }

        return header.substring(BEARER_PREFIX.length()).trim();
    }
}
