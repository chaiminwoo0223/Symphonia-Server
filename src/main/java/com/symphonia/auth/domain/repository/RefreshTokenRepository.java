package com.symphonia.auth.domain.repository;

import java.util.Optional;

public interface RefreshTokenRepository {
    void save(String value, String memberId, Long expirationTime);

    Optional<String> findMemberIdByValue(String value);

    // findMemberIdByValue와 달리 동시에 같은 토큰으로 들어온 요청 중 하나만 성공하도록 보장하고,
    // 성공한 요청은 해당 토큰만 삭제한다. 같은 회원의 다른 토큰은 유지된다.
    Optional<String> consume(String value);

    void delete(String memberId);
}
