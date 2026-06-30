package com.symphonia.member.application.service;

import com.symphonia.UnitTest;
import com.symphonia.global.exception.BusinessException;
import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.entity.SocialProvider;
import com.symphonia.member.domain.error.MemberErrorCode;
import com.symphonia.member.domain.repository.MemberRepository;
import com.symphonia.member.fixture.MemberFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@DisplayName("MemberQueryService 단위 테스트")
class MemberQueryServiceTest extends UnitTest {

    @InjectMocks
    private MemberQueryService memberQueryService;

    @Mock
    private MemberRepository memberRepository;

    private Member activeGoogleMember;
    private Member activeAppleMember;

    @BeforeEach
    void setUp() {
        activeGoogleMember = MemberFixture.GOOGLE.create();
        activeAppleMember = MemberFixture.APPLE.create();
    }

    @Nested
    @DisplayName("getActiveBySocialLogin 메서드는")
    class GetActiveBySocialLogin {
        private static final String UNKNOWN_SOCIAL_ID = "xxx";

        @Nested
        @DisplayName("SocialProvider가 GOOGLE인 경우")
        class Google {

            @Test
            @DisplayName("멤버를 찾을 수 없으면 예외가 발생한다.")
            void shouldThrowExceptionWhenMemberNotFound() {
                // given
                given(memberRepository.findBySocialLogin(SocialProvider.GOOGLE, UNKNOWN_SOCIAL_ID))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> memberQueryService.getActiveBySocialLogin(SocialProvider.GOOGLE, UNKNOWN_SOCIAL_ID))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
            }

            @Test
            @DisplayName("활성 멤버를 조회하면 MemberResult를 반환한다.")
            void shouldReturnMemberResultWhenActiveMemberFound() {
                // given
                given(memberRepository.findBySocialLogin(SocialProvider.GOOGLE, activeGoogleMember.getSocialId()))
                        .willReturn(Optional.of(activeGoogleMember));

                // when
                MemberResult result = memberQueryService.getActiveBySocialLogin(SocialProvider.GOOGLE, activeGoogleMember.getSocialId());

                // then
                assertThat(result.socialId()).isEqualTo(activeGoogleMember.getSocialId());
                assertThat(result.nickname()).isEqualTo(activeGoogleMember.getNickname());
                assertThat(result.email()).isEqualTo(activeGoogleMember.getEmail());
                assertThat(result.profileImage()).isEqualTo(activeGoogleMember.getProfileImage());
                assertThat(result.role()).isEqualTo(activeGoogleMember.getRole());
                assertThat(result.socialProvider()).isEqualTo(activeGoogleMember.getSocialProvider());
            }
        }

        @Nested
        @DisplayName("SocialProvider가 APPLE인 경우")
        class Apple {

            @Test
            @DisplayName("멤버를 찾을 수 없으면 예외가 발생한다.")
            void shouldThrowExceptionWhenMemberNotFound() {
                // given
                given(memberRepository.findBySocialLogin(SocialProvider.APPLE, UNKNOWN_SOCIAL_ID))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> memberQueryService.getActiveBySocialLogin(SocialProvider.APPLE, UNKNOWN_SOCIAL_ID))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
            }

            @Test
            @DisplayName("활성 멤버를 조회하면 MemberResult를 반환한다.")
            void shouldReturnMemberResultWhenActiveMemberFound() {
                // given
                given(memberRepository.findBySocialLogin(SocialProvider.APPLE, activeAppleMember.getSocialId()))
                        .willReturn(Optional.of(activeAppleMember));

                // when
                MemberResult result = memberQueryService.getActiveBySocialLogin(SocialProvider.APPLE, activeAppleMember.getSocialId());

                // then
                assertThat(result.socialId()).isEqualTo(activeAppleMember.getSocialId());
                assertThat(result.nickname()).isEqualTo(activeAppleMember.getNickname());
                assertThat(result.email()).isEqualTo(activeAppleMember.getEmail());
                assertThat(result.profileImage()).isEqualTo(activeAppleMember.getProfileImage());
                assertThat(result.role()).isEqualTo(activeAppleMember.getRole());
                assertThat(result.socialProvider()).isEqualTo(activeAppleMember.getSocialProvider());
            }
        }
    }

    @Nested
    @DisplayName("getActiveById 메서드는")
    class GetActiveById {

        @Test
        @DisplayName("멤버를 찾을 수 없으면 예외가 발생한다.")
        void shouldThrowExceptionWhenMemberNotFound() {
            // given
            Long unknownId = -1L;
            given(memberRepository.findById(unknownId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberQueryService.getActiveById(unknownId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("ID로 활성 멤버를 조회하면 MemberResult를 반환한다.")
        void shouldReturnMemberResultWhenActiveMemberFound() {
            // given
            Long memberId = 1L;
            given(memberRepository.findById(memberId))
                    .willReturn(Optional.of(activeGoogleMember));

            // when
            MemberResult result = memberQueryService.getActiveById(memberId);

            // then
            assertThat(result.socialId()).isEqualTo(activeGoogleMember.getSocialId());
            assertThat(result.nickname()).isEqualTo(activeGoogleMember.getNickname());
            assertThat(result.email()).isEqualTo(activeGoogleMember.getEmail());
            assertThat(result.profileImage()).isEqualTo(activeGoogleMember.getProfileImage());
            assertThat(result.role()).isEqualTo(activeGoogleMember.getRole());
            assertThat(result.socialProvider()).isEqualTo(activeGoogleMember.getSocialProvider());
        }
    }
}
