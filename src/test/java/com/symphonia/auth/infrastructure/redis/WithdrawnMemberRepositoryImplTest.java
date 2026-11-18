package com.symphonia.auth.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RedisRepositoryTest;
import com.symphonia.auth.domain.repository.WithdrawnMemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

@Import(WithdrawnMemberRepositoryImpl.class)
class WithdrawnMemberRepositoryImplTest extends RedisRepositoryTest {

    private static final String MEMBER_ID = "1";
    private static final Long EXPIRATION_TIME = 7200L;

    @Autowired private WithdrawnMemberRepository withdrawnMemberRepository;
    @Autowired private StringRedisTemplate redisTemplate;

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Test
        @DisplayName("탈퇴한 회원 id를 저장한다")
        void shouldSaveWithdrawnMemberId() {
            // when
            withdrawnMemberRepository.save(MEMBER_ID, EXPIRATION_TIME);

            // then
            assertThat(withdrawnMemberRepository.isWithdrawn(MEMBER_ID)).isTrue();
        }

        @Test
        @DisplayName("전달한 유효시간이 지나면 만료되도록 저장한다")
        void shouldExpireAfterGivenExpirationTime() {
            // when
            withdrawnMemberRepository.save(MEMBER_ID, EXPIRATION_TIME);

            // then
            Long ttl = redisTemplate.getExpire("withdrawn_member:" + MEMBER_ID);
            assertThat(ttl).isPositive().isLessThanOrEqualTo(EXPIRATION_TIME);
        }
    }

    @Nested
    @DisplayName("isWithdrawn 메서드는")
    class IsWithdrawn {

        @Test
        @DisplayName("저장하지 않은 회원이면 false를 반환한다")
        void shouldReturnFalseWhenMemberNotSaved() {
            // when
            boolean result = withdrawnMemberRepository.isWithdrawn("unknown");

            // then
            assertThat(result).isFalse();
        }
    }
}
