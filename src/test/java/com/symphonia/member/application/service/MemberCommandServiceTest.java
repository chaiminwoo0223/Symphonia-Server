package com.symphonia.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.member.application.dto.command.CreateMemberCommand;
import com.symphonia.member.application.dto.command.DeleteMemberCommand;
import com.symphonia.member.application.dto.command.UpdateMemberCommand;
import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.application.event.MemberDeletedEvent;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.entity.SocialProvider;
import com.symphonia.member.domain.error.MemberErrorCode;
import com.symphonia.member.domain.exception.MemberAlreadyExistsException;
import com.symphonia.member.domain.exception.MemberNotFoundException;
import com.symphonia.member.domain.repository.MemberRepository;
import com.symphonia.member.fixture.CreateMemberCommandFixture;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.fixture.UpdateMemberCommandFixture;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

@DisplayName("MemberCommandService 단위 테스트")
class MemberCommandServiceTest extends UnitTest {

    private static final String ACCESS_TOKEN = "access-token";
    private static final String IP = "127.0.0.1";

    @InjectMocks private MemberCommandService memberCommandService;

    @Mock private MemberRepository memberRepository;

    @Mock private ApplicationEventPublisher eventPublisher;

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
        private CreateMemberCommand command;

        @BeforeEach
        void setUp() {
            command = new CreateMemberCommandFixture(MemberFixture.KAKAO).build();
        }

        @Nested
        @DisplayName("이미 가입된 멤버가 존재하는 경우")
        class WhenAlreadyExists {

            @Test
            @DisplayName("MemberAlreadyExistsException이 발생한다.")
            void shouldThrowMemberAlreadyExistsException() {
                // given
                given(
                                memberRepository.existsBySocialLogin(
                                        SocialProvider.KAKAO, kakaoMember.getSocialId()))
                        .willReturn(true);

                // when & then
                assertThatThrownBy(() -> memberCommandService.create(command))
                        .isInstanceOf(MemberAlreadyExistsException.class)
                        .hasMessage(MemberErrorCode.MEMBER_ALREADY_EXISTS.getMessage());
            }
        }

        @Nested
        @DisplayName("이미 가입된 멤버가 없는 경우")
        class WhenNotAlreadyExists {

            @Test
            @DisplayName("MemberResult를 반환한다.")
            void shouldReturnMemberResult() {
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
    }

    @Nested
    @DisplayName("update 메서드는")
    class Update {
        private UpdateMemberCommand command;

        @BeforeEach
        void setUp() {
            command = new UpdateMemberCommandFixture().nickname("팬텀").build();
        }

        @Test
        @DisplayName("멤버를 찾을 수 없으면 MemberNotFoundException이 발생한다.")
        void shouldThrowMemberNotFoundException() {
            // given
            Long unknownId = -1L;
            given(memberRepository.findById(unknownId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberCommandService.update(unknownId, command))
                    .isInstanceOf(MemberNotFoundException.class)
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
        @DisplayName("멤버를 찾을 수 없으면 MemberNotFoundException이 발생한다.")
        void shouldThrowMemberNotFoundException() {
            // given
            Long unknownId = -1L;
            given(memberRepository.findById(unknownId)).willReturn(Optional.empty());
            DeleteMemberCommand command = DeleteMemberCommand.of(unknownId, ACCESS_TOKEN, IP);

            // when & then
            assertThatThrownBy(() -> memberCommandService.delete(command))
                    .isInstanceOf(MemberNotFoundException.class)
                    .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("멤버가 존재하면 멤버를 삭제하고 MemberDeletedEvent를 발행한다.")
        void shouldDeleteMemberAndPublishEventWhenMemberExists() {
            // given
            Long memberId = 1L;
            given(memberRepository.findById(memberId)).willReturn(Optional.of(googleMember));
            DeleteMemberCommand command = DeleteMemberCommand.of(memberId, ACCESS_TOKEN, IP);

            // when
            memberCommandService.delete(command);

            // then
            then(memberRepository).should().delete(googleMember);
            ArgumentCaptor<MemberDeletedEvent> eventCaptor =
                    ArgumentCaptor.forClass(MemberDeletedEvent.class);
            then(eventPublisher).should().publishEvent(eventCaptor.capture());
            MemberDeletedEvent event = eventCaptor.getValue();
            assertThat(event.memberId()).isEqualTo(memberId);
            assertThat(event.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(event.ip()).isEqualTo(IP);
        }
    }
}
