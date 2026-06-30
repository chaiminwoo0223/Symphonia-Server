package com.symphonia.member.application.service;

import com.symphonia.UnitTest;
import com.symphonia.global.exception.BusinessException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@DisplayName("MemberCommandService 단위 테스트")
class MemberCommandServiceTest extends UnitTest {

    @InjectMocks
    private MemberCommandService memberCommandService;

    @Mock
    private MemberRepository memberRepository;

    private Member activeGoogleMember;
    private Member withdrawnGoogleMember;
    private Member activeAppleMember;
    private Member withdrawnAppleMember;

    @BeforeEach
    void setUp() {
        activeGoogleMember = MemberFixture.GOOGLE.toActive();
        withdrawnGoogleMember = MemberFixture.GOOGLE.toWithdrawn();
        activeAppleMember = MemberFixture.APPLE.toActive();
        withdrawnAppleMember = MemberFixture.APPLE.toWithdrawn();
    }

    @Nested
    @DisplayName("create 메서드는")
    class Create {

        @Nested
        @DisplayName("SocialProvider가 GOOGLE인 경우")
        class Google {
            private MemberCreateCommand command;

            @BeforeEach
            void setUp() {
                command = new MemberCreateCommandFixture(MemberFixture.GOOGLE).build();
            }

            @Test
            @DisplayName("이미 가입된 멤버가 존재하면 예외가 발생한다.")
            void shouldThrowExceptionWhenMemberAlreadyExists() {
                // given
                given(memberRepository.existsBySocialLogin(SocialProvider.GOOGLE, activeGoogleMember.getSocialId()))
                        .willReturn(true);

                // when & then
                assertThatThrownBy(() -> memberCommandService.create(command))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(MemberErrorCode.MEMBER_ALREADY_EXISTS.getMessage());
            }

            @Test
            @DisplayName("유효한 커맨드로 멤버를 생성하면 MemberResult를 반환한다.")
            void shouldReturnMemberResultWhenMemberCreatedWithValidCommand() {
                // given
                given(memberRepository.existsBySocialLogin(SocialProvider.GOOGLE, activeGoogleMember.getSocialId()))
                        .willReturn(false);
                given(memberRepository.save(any(Member.class)))
                        .willReturn(activeGoogleMember);

                // when
                MemberResult result = memberCommandService.create(command);

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
            private MemberCreateCommand command;

            @BeforeEach
            void setUp() {
                command = new MemberCreateCommandFixture(MemberFixture.APPLE).build();
            }

            @Test
            @DisplayName("이미 가입된 멤버가 존재하면 예외가 발생한다.")
            void shouldThrowExceptionWhenMemberAlreadyExists() {
                // given
                given(memberRepository.existsBySocialLogin(SocialProvider.APPLE, activeAppleMember.getSocialId()))
                        .willReturn(true);

                // when & then
                assertThatThrownBy(() -> memberCommandService.create(command))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(MemberErrorCode.MEMBER_ALREADY_EXISTS.getMessage());
            }

            @Test
            @DisplayName("유효한 커맨드로 멤버를 생성하면 MemberResult를 반환한다.")
            void shouldReturnMemberResultWhenMemberCreatedWithValidCommand() {
                // given
                given(memberRepository.existsBySocialLogin(SocialProvider.APPLE, activeAppleMember.getSocialId()))
                        .willReturn(false);
                given(memberRepository.save(any(Member.class)))
                        .willReturn(activeAppleMember);

                // when
                MemberResult result = memberCommandService.create(command);

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
            given(memberRepository.findById(unknownId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberCommandService.update(unknownId, command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("탈퇴한 멤버를 수정하면 예외가 발생한다.")
        void shouldThrowExceptionWhenMemberAlreadyWithdrawn() {
            // given
            Long withdrawnId = 1L;
            given(memberRepository.findById(withdrawnId))
                    .willReturn(Optional.of(withdrawnGoogleMember));

            // when & then
            assertThatThrownBy(() -> memberCommandService.update(withdrawnId, command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(MemberErrorCode.MEMBER_ALREADY_WITHDRAWN.getMessage());
        }

        @Test
        @DisplayName("활성 멤버를 수정하면 MemberResult를 반환한다.")
        void shouldReturnMemberResultWhenActiveMemberUpdated() {
            // given
            Long memberId = 1L;
            given(memberRepository.findById(memberId))
                    .willReturn(Optional.of(activeGoogleMember));

            // when
            MemberResult result = memberCommandService.update(memberId, command);

            // then
            assertThat(result.nickname()).isEqualTo(activeGoogleMember.getNickname());
        }
    }

    @Nested
    @DisplayName("withdraw 메서드는")
    class Withdraw {

        @Test
        @DisplayName("멤버를 찾을 수 없으면 예외가 발생한다.")
        void shouldThrowExceptionWhenMemberNotFound() {
            // given
            Long unknownId = -1L;
            given(memberRepository.findById(unknownId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberCommandService.delete(unknownId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("탈퇴한 멤버를 탈퇴시키면 예외가 발생한다.")
        void shouldThrowExceptionWhenMemberAlreadyWithdrawn() {
            // given
            Long withdrawnId = 1L;
            given(memberRepository.findById(withdrawnId))
                    .willReturn(Optional.of(withdrawnGoogleMember));

            // when & then
            assertThatThrownBy(() -> memberCommandService.delete(withdrawnId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(MemberErrorCode.MEMBER_ALREADY_WITHDRAWN.getMessage());
        }

        @Test
        @DisplayName("활성 멤버가 탈퇴하면 deletedAt이 설정된다.")
        void shouldSetDeletedAtWhenActiveMemberWithdrawn() {
            // given
            Long memberId = 1L;
            given(memberRepository.findById(memberId))
                    .willReturn(Optional.of(activeGoogleMember));

            // when
            memberCommandService.delete(memberId);

            // then
            assertThat(activeGoogleMember.isDeleted()).isTrue();
        }
    }
}
