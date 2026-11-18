package com.symphonia.auth.domain.repository;

public interface WithdrawnMemberRepository {
    void save(String memberId, Long expirationTime);

    boolean isWithdrawn(String memberId);
}
