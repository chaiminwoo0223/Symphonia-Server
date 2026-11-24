package com.symphonia.auth.infrastructure.redis;

import static com.symphonia.auth.infrastructure.redis.RedisFailureTranslator.call;
import static com.symphonia.auth.infrastructure.redis.RedisFailureTranslator.run;

import com.symphonia.auth.domain.repository.WithdrawnMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WithdrawnMemberRepositoryImpl implements WithdrawnMemberRepository {
    private final WithdrawnMemberRedisRepository withdrawnMemberRedisRepository;

    @Override
    public void save(String memberId, Long expirationTime) {
        run(
                () ->
                        withdrawnMemberRedisRepository.save(
                                WithdrawnMember.of(memberId, expirationTime)));
    }

    @Override
    public boolean isWithdrawn(String memberId) {
        return call(() -> withdrawnMemberRedisRepository.existsById(memberId));
    }
}
