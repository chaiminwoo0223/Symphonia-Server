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

    private Member kakaoMember;
    private Member googleMember;
    private Member appleMember;

    @BeforeEach
    void setUp() {
        kakaoMember = MemberFixture.KAKAO.create();
        googleMember = MemberFixture.GOOGLE.create();
        appleMember = MemberFixture.APPLE.create();
    }

    @Nested
    @DisplayName("getBySocialLogin 메서드는")
    class GetBySocialLogin {
        private static final String UNKNOWN_SOCIAL_ID = "xxx";

        @Nested
        @DisplayName("SocialProvider가 KAKAO인 경우")
        class KAKAO {

            @Test
            @DisplayName("멤버를 찾을 수 없으면 예외가 발생한다.")
            void shouldThrowExceptionWhenMemberNotFound() {
                // given
                given(memberRepository.findBySocialLogin(SocialProvider.KAKAO, UNKNOWN_SOCIAL_ID))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> memberQueryService.getBySocialLogin(SocialProvider.KAKAO, UNKNOWN_SOCIAL_ID))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
            }

            @Test
            @DisplayName("멤버가 존재하면 MemberResult를 반환한다.")
            void shouldReturnMemberResultWhenFoundBySocialLogin() {
                // given
                given(memberRepository.findBySocialLogin(SocialProvider.KAKAO, kakaoMember.getSocialId()))
                        .willReturn(Optional.of(kakaoMember));

                // when
                MemberResult result = memberQueryService.getBySocialLogin(SocialProvider.KAKAO, kakaoMember.getSocialId());

                // then
                assertThat(result.socialId()).isEqualTo(kakaoMember.getSocialId());
                assertThat(result.nickname()).isEqualTo(kakaoMember.getNickname());
                assertThat(result.email()).isEqualTo(kakaoMember.getEmail());
                assertThat(result.profileImage()).isEqualTo(kakaoMember.getProfileImage());
                assertThat(result.role()).isEqualTo(kakaoMember.getRole());
                assertThat(result.socialProvider()).isEqualTo(kakaoMember.getSocialProvider());
            }
        }

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
                assertThatThrownBy(() -> memberQueryService.getBySocialLogin(SocialProvider.GOOGLE, UNKNOWN_SOCIAL_ID))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
            }

            @Test
            @DisplayName("멤버가 존재하면 MemberResult를 반환한다.")
            void shouldReturnMemberResultWhenFoundBySocialLogin() {
                // given
                given(memberRepository.findBySocialLogin(SocialProvider.GOOGLE, googleMember.getSocialId()))
                        .willReturn(Optional.of(googleMember));

                // when
                MemberResult result = memberQueryService.getBySocialLogin(SocialProvider.GOOGLE, googleMember.getSocialId());

                // then
                assertThat(result.socialId()).isEqualTo(googleMember.getSocialId());
                assertThat(result.nickname()).isEqualTo(googleMember.getNickname());
                assertThat(result.email()).isEqualTo(googleMember.getEmail());
                assertThat(result.profileImage()).isEqualTo(googleMember.getProfileImage());
                assertThat(result.role()).isEqualTo(googleMember.getRole());
                assertThat(result.socialProvider()).isEqualTo(googleMember.getSocialProvider());
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
                assertThatThrownBy(() -> memberQueryService.getBySocialLogin(SocialProvider.APPLE, UNKNOWN_SOCIAL_ID))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
            }

            @Test
            @DisplayName("멤버가 존재하면 MemberResult를 반환한다.")
            void shouldReturnMemberResultWhenFoundBySocialLogin() {
                // given
                given(memberRepository.findBySocialLogin(SocialProvider.APPLE, appleMember.getSocialId()))
                        .willReturn(Optional.of(appleMember));

                // when
                MemberResult result = memberQueryService.getBySocialLogin(SocialProvider.APPLE, appleMember.getSocialId());

                // then
                assertThat(result.socialId()).isEqualTo(appleMember.getSocialId());
                assertThat(result.nickname()).isEqualTo(appleMember.getNickname());
                assertThat(result.email()).isEqualTo(appleMember.getEmail());
                assertThat(result.profileImage()).isEqualTo(appleMember.getProfileImage());
                assertThat(result.role()).isEqualTo(appleMember.getRole());
                assertThat(result.socialProvider()).isEqualTo(appleMember.getSocialProvider());
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
            assertThatThrownBy(() -> memberQueryService.getById(unknownId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("멤버가 존재하면 MemberResult를 반환한다.")
        void shouldReturnMemberResultWhenFoundById() {
            // given
            Long memberId = 1L;
            given(memberRepository.findById(memberId))
                    .willReturn(Optional.of(googleMember));

            // when
            MemberResult result = memberQueryService.getById(memberId);

            // then
            assertThat(result.socialId()).isEqualTo(googleMember.getSocialId());
            assertThat(result.nickname()).isEqualTo(googleMember.getNickname());
            assertThat(result.email()).isEqualTo(googleMember.getEmail());
            assertThat(result.profileImage()).isEqualTo(googleMember.getProfileImage());
            assertThat(result.role()).isEqualTo(googleMember.getRole());
            assertThat(result.socialProvider()).isEqualTo(googleMember.getSocialProvider());
        }
    }
}
