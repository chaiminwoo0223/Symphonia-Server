package com.symphonia.auth.domain.repository;

import java.util.Optional;

public interface RefreshTokenRepository {
    void save(String value, String memberId, Long expirationTime);

    Optional<String> findMemberIdByValue(String value);

    // findMemberIdByValue와 달리 동시에 같은 토큰으로 들어온 요청 중 하나만 성공하도록 보장한다.
    Optional<String> consume(String value);

    void delete(String memberId);
}
