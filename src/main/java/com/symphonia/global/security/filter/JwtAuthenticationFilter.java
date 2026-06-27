package com.symphonia.global.security.filter;

import com.symphonia.auth.domain.repository.BlacklistAccessTokenRepository;
import com.symphonia.auth.infrastructure.provider.AccessTokenProvider;
import com.symphonia.global.common.constants.HttpConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final AccessTokenProvider accessTokenProvider;
    private final BlacklistAccessTokenRepository blacklistAccessTokenRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String accessToken = extractBearerToken(request);

        if (StringUtils.hasText(accessToken) && isValidToken(accessToken)) {
            setAuthentication(accessToken);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValidToken(String accessToken) {
        return accessTokenProvider.validate(accessToken) && !blacklistAccessTokenRepository.isBlacklisted(accessToken);
    }

    private void setAuthentication(String accessToken) {
        String memberId = accessTokenProvider.getMemberId(accessToken);
        String role = accessTokenProvider.getRole(accessToken);

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(memberId, accessToken, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(header) || !header.startsWith(HttpConstants.BEARER_PREFIX)) {
            return null;
        }

        return header.substring(HttpConstants.BEARER_PREFIX.length()).trim();
    }
}
