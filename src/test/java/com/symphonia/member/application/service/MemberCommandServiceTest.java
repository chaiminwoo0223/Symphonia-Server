package com.symphonia.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.common.exception.BusinessException;
import com.symphonia.member.application.dto.command.MemberCreateCommand;
import com.symphonia.member.application.dto.command.MemberUpdateCommand;
import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.entity.SocialProvider;
import com.symphonia.member.domain.error.MemberErrorCode;
import com.symphonia.member.domain.repository.MemberRepository;
import com.symphonia.member.fixture.MemberCreateCommandFixture;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.fixture.MemberUpdateCommandFixture;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("MemberCommandService 단위 테스트")
class MemberCommandServiceTest extends UnitTest {

    @InjectMocks private MemberCommandService memberCommandService;

    @Mock private MemberRepository memberRepository;

    private Member kakaoMember;
    private Member googleMember;

    @BeforeEach
    void setUp() {
        kakaoMember = MemberFixture.KAKAO.create();
        googleMember = MemberFixture.GOOGLE.create();
    }

    @Nested
    @DisplayName("create 메서드는")
    class Create {

        @Nested
        @DisplayName("SocialProvider가 KAKAO인 경우")
        class WhenSocialProviderIsKakao {
            private MemberCreateCommand command;

            @BeforeEach
            void setUp() {
                command = new MemberCreateCommandFixture(MemberFixture.KAKAO).build();
            }

            @Test
            @DisplayName("이미 가입된 멤버가 존재하면 예외가 발생한다.")
            void shouldThrowExceptionWhenAlreadyExistsBySocialLogin() {
                // given
                given(
                                memberRepository.existsBySocialLogin(
                                        SocialProvider.KAKAO, kakaoMember.getSocialId()))
                        .willReturn(true);

                // when & then
                assertThatThrownBy(() -> memberCommandService.create(command))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(MemberErrorCode.MEMBER_ALREADY_EXISTS.getMessage());
            }

            @Test
            @DisplayName("이미 가입된 멤버가 없으면 MemberResult를 반환한다.")
            void shouldReturnMemberResultWhenNotAlreadyExistsBySocialLogin() {
                // given
                given(
                                memberRepository.existsBySocialLogin(
                                        SocialProvider.KAKAO, kakaoMember.getSocialId()))
                        .willReturn(false);
                given(memberRepository.save(any(Member.class))).willReturn(kakaoMember);

                // when
                MemberResult result = memberCommandService.create(command);

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
        class WhenSocialProviderIsGoogle {
            private MemberCreateCommand command;

            @BeforeEach
            void setUp() {
                command = new MemberCreateCommandFixture(MemberFixture.GOOGLE).build();
            }

            @Test
            @DisplayName("이미 가입된 멤버가 존재하면 예외가 발생한다.")
            void shouldThrowExceptionWhenAlreadyExistsBySocialLogin() {
                // given
                given(
                                memberRepository.existsBySocialLogin(
                                        SocialProvider.GOOGLE, googleMember.getSocialId()))
                        .willReturn(true);

                // when & then
                assertThatThrownBy(() -> memberCommandService.create(command))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(MemberErrorCode.MEMBER_ALREADY_EXISTS.getMessage());
            }

            @Test
            @DisplayName("이미 가입된 멤버가 없으면 MemberResult를 반환한다.")
            void shouldReturnMemberResultWhenNotAlreadyExistsBySocialLogin() {
                // given
                given(
                                memberRepository.existsBySocialLogin(
                                        SocialProvider.GOOGLE, googleMember.getSocialId()))
                        .willReturn(false);
                given(memberRepository.save(any(Member.class))).willReturn(googleMember);

                // when
                MemberResult result = memberCommandService.create(command);

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

    @Nested
    @DisplayName("update 메서드는")
    class Update {
        private MemberUpdateCommand command;

        @BeforeEach
        void setUp() {
            command = new MemberUpdateCommandFixture().nickname("팬텀").build();
        }

        @Test
        @DisplayName("멤버를 찾을 수 없으면 예외가 발생한다.")
        void shouldThrowExceptionWhenMemberNotFound() {
            // given
            Long unknownId = -1L;
            given(memberRepository.findById(unknownId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberCommandService.update(unknownId, command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("멤버가 존재하면 MemberResult를 반환한다.")
        void shouldReturnMemberResultWhenMemberExists() {
            // given
            Long memberId = 1L;
            given(memberRepository.findById(memberId)).willReturn(Optional.of(googleMember));
            given(memberRepository.save(googleMember)).willReturn(googleMember);

            // when
            MemberResult result = memberCommandService.update(memberId, command);

            // then
            assertThat(result.nickname()).isEqualTo(command.nickname());
            then(memberRepository).should().save(googleMember);
        }
    }

    @Nested
    @DisplayName("delete 메서드는")
    class Delete {

        @Test
        @DisplayName("멤버를 찾을 수 없으면 예외가 발생한다.")
        void shouldThrowExceptionWhenMemberNotFound() {
            // given
            Long unknownId = -1L;
            given(memberRepository.findById(unknownId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberCommandService.delete(unknownId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("멤버가 존재하면 멤버를 삭제한다.")
        void shouldDeleteMemberWhenMemberExists() {
            // given
            Long memberId = 1L;
            given(memberRepository.findById(memberId)).willReturn(Optional.of(googleMember));

            // when
            memberCommandService.delete(memberId);

            // then
            then(memberRepository).should().delete(googleMember);
        }
    }
}
