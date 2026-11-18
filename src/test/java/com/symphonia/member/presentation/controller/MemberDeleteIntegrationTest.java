package com.symphonia.member.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.auth.application.usecase.BlockWithdrawnMemberUseCase;
import com.symphonia.auth.helper.AuthHelper;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.repository.MemberRepository;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 탈퇴 후속 처리(커밋 직전과 커밋 뒤 리스너)를 트랜잭션 롤백 없이 실제로 커밋해서 확인한다.
class MemberDeleteIntegrationTest extends IntegrationTest {

    @Autowired private MemberHelper memberHelper;
    @Autowired private AuthHelper authHelper;
    @Autowired private MemberRepository memberRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private BlockWithdrawnMemberUseCase blockWithdrawnMemberUseCase;

    @Nested
    @DisplayName("DELETE /api/v1/members/me는")
    class Delete {

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("탈퇴한 회원이 낸 페어링 피드백의 회원 id를 같은 트랜잭션에서 비운다")
        void shouldAnonymizePairingFeedbackOfDeletedMember() throws Exception {
            // given
            Member member = memberHelper.save(MemberFixture.KAKAO);
            String accessToken =
                    authHelper.generateAccessToken(
                            String.valueOf(member.getId()), member.getRole().name());
            Long feedbackId = insertPairingFeedbackOf(member.getId());

            try {
                // when
                mockMvc.perform(
                                delete("/api/v1/members/me")
                                        .header(
                                                HttpHeaders.AUTHORIZATION,
                                                authHelper.bearerHeader(accessToken)))
                        .andExpect(status().isNoContent());

                // then
                Long memberId =
                        jdbcTemplate.queryForObject(
                                "SELECT member_id FROM pairing_feedback WHERE id = ?",
                                Long.class,
                                feedbackId);
                assertThat(memberId).isNull();
            } finally {
                jdbcTemplate.update("DELETE FROM pairing_feedback WHERE id = ?", feedbackId);
            }
        }

        @Nested
        @DisplayName("탈퇴 후속 처리가 실패하는 경우")
        class WhenPostCommitHandlingFails {

            @Test
            @Transactional(propagation = Propagation.NOT_SUPPORTED)
            @DisplayName("이미 커밋된 회원 삭제를 유지하고 204를 반환한다")
            void shouldKeepDeletionAndReturnNoContent() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String accessToken =
                        authHelper.generateAccessToken(
                                String.valueOf(member.getId()), member.getRole().name());
                willThrow(new IllegalStateException("Redis 장애"))
                        .given(blockWithdrawnMemberUseCase)
                        .block(any());

                // when
                mockMvc.perform(
                                delete("/api/v1/members/me")
                                        .header(
                                                HttpHeaders.AUTHORIZATION,
                                                authHelper.bearerHeader(accessToken)))
                        .andExpect(status().isNoContent());

                // then
                assertThat(memberRepository.findById(member.getId())).isEmpty();
            }
        }
    }

    // 시드 마이그레이션이 적재한 음료와 안주의 id를 쓴다. 카탈로그를 지우면 다른 테스트가 깨지므로 건드리지 않는다.
    private Long insertPairingFeedbackOf(Long memberId) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO pairing_feedback
                    (member_id, drink_id, anju_id, relationship_type, rating, created_at, updated_at)
                VALUES (?, (SELECT MIN(id) FROM drink), (SELECT MIN(id) FROM anju),
                        'FRIEND', 'LIKE', now(), now())
                RETURNING id
                """,
                Long.class,
                memberId);
    }
}
