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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 탈퇴 후속 처리(AFTER_COMMIT 리스너)가 실패했을 때의 응답을 확인한다.
// 리스너는 커밋이 끝난 뒤에 실행되므로 트랜잭션 롤백을 쓰지 않고 실제로 커밋한다.
class MemberDeleteFailureIntegrationTest extends IntegrationTest {

    @Autowired private MemberHelper memberHelper;
    @Autowired private AuthHelper authHelper;
    @Autowired private MemberRepository memberRepository;

    @MockitoBean private BlockWithdrawnMemberUseCase blockWithdrawnMemberUseCase;

    @Nested
    @DisplayName("DELETE /api/v1/members/me는")
    class Delete {

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
}
