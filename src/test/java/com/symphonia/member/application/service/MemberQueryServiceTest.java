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
    private Member deletedGoogleMember;
    private Member activeAppleMember;
    private Member deletedAppleMember;

    @BeforeEach
    void setUp() {
        activeGoogleMember = MemberFixture.GOOGLE.toActive();
        deletedGoogleMember = MemberFixture.GOOGLE.toDeleted();
        activeAppleMember = MemberFixture.APPLE.toActive();
        deletedAppleMember = MemberFixture.APPLE.toDeleted();
    }

    @Nested
    @DisplayName("(GOOGLE) getActiveBySocialLogin 메서드는")
    class GetActiveByGoogleLogin {

        @Test
        @DisplayName("멤버를 찾을 수 없으면 예외가 발생한다.")
        void shouldThrowExceptionWhenMemberNotFound() {
            // given
            String unknownSocialId = "xxx";
            given(memberRepository.findBySocialLogin(SocialProvider.GOOGLE, unknownSocialId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberQueryService.getActiveBySocialLogin(SocialProvider.GOOGLE, unknownSocialId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("삭제된 멤버를 조회하면 예외가 발생한다.")
        void shouldThrowExceptionWhenMemberAlreadyDeleted() {
            // given
            given(memberRepository.findBySocialLogin(SocialProvider.GOOGLE, deletedGoogleMember.getSocialId()))
                    .willReturn(Optional.of(deletedGoogleMember));

            // when & then
            assertThatThrownBy(() -> memberQueryService.getActiveBySocialLogin(SocialProvider.GOOGLE, deletedGoogleMember.getSocialId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(MemberErrorCode.MEMBER_ALREADY_WITHDRAWN.getMessage());
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
    @DisplayName("(APPLE) getActiveBySocialLogin 메서드는")
    class GetActiveByAppleLogin {

        @Test
        @DisplayName("멤버를 찾을 수 없으면 예외가 발생한다.")
        void shouldThrowExceptionWhenMemberNotFound() {
            // given
            String unknownSocialId = "xxx";
            given(memberRepository.findBySocialLogin(SocialProvider.APPLE, unknownSocialId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberQueryService.getActiveBySocialLogin(SocialProvider.APPLE, unknownSocialId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("삭제된 멤버를 조회하면 예외가 발생한다.")
        void shouldThrowExceptionWhenMemberAlreadyDeleted() {
            // given
            given(memberRepository.findBySocialLogin(SocialProvider.APPLE, deletedAppleMember.getSocialId()))
                    .willReturn(Optional.of(deletedAppleMember));

            // when & then
            assertThatThrownBy(() -> memberQueryService.getActiveBySocialLogin(SocialProvider.APPLE, deletedAppleMember.getSocialId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(MemberErrorCode.MEMBER_ALREADY_WITHDRAWN.getMessage());
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
        @DisplayName("탈퇴한 멤버를 조회하면 예외가 발생한다.")
        void shouldThrowExceptionWhenMemberAlreadyDeleted() {
            // given
            Long withdrawnId = 1L;
            given(memberRepository.findById(withdrawnId))
                    .willReturn(Optional.of(deletedGoogleMember));

            // when & then
            assertThatThrownBy(() -> memberQueryService.getActiveById(withdrawnId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(MemberErrorCode.MEMBER_ALREADY_WITHDRAWN.getMessage());
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
