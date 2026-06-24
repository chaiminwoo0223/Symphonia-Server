package com.symphonia.member.application.service;

import com.symphonia.UnitTest;
import com.symphonia.global.exception.BusinessException;
import com.symphonia.member.application.dto.command.MemberCreateCommand;
import com.symphonia.member.application.dto.command.MemberRestoreCommand;
import com.symphonia.member.application.dto.command.MemberUpdateCommand;
import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.entity.SocialProvider;
import com.symphonia.member.domain.error.MemberErrorCode;
import com.symphonia.member.domain.repository.MemberRepository;
import com.symphonia.member.fixture.MemberCreateCommandFixture;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.fixture.MemberRestoreCommandFixture;
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
    private Member activeAppleMember;
    private Member deletedGoogleMember;
    private Member deletedAppleMember;

    @BeforeEach
    void setUp() {
        activeGoogleMember = MemberFixture.GOOGLE.toActive();
        activeAppleMember = MemberFixture.APPLE.toActive();
        deletedGoogleMember = MemberFixture.GOOGLE.toDeleted();
        deletedAppleMember = MemberFixture.APPLE.toDeleted();
    }

    @Nested
    @DisplayName("(GOOGLE) create 메서드는")
    class CreateByGoogle {
        private MemberCreateCommand command;

        @BeforeEach
        void setUp() {
            MemberCreateCommandFixture commandFixture = new MemberCreateCommandFixture(MemberFixture.GOOGLE);
            command = commandFixture.build();
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
    @DisplayName("(APPLE) create 메서드는")
    class CreateByApple {
        private MemberCreateCommand command;

        @BeforeEach
        void setUp() {
            MemberCreateCommandFixture commandFixture = new MemberCreateCommandFixture(MemberFixture.APPLE);
            command = commandFixture.build();
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

    @Nested
    @DisplayName("update 메서드는")
    class Update {
        private MemberUpdateCommand command;

        @BeforeEach
        void setUp() {
            MemberUpdateCommandFixture commandFixture = new MemberUpdateCommandFixture();
            command = commandFixture.nickname("팬텀").profileImage("https://image.symphonia.com/profile/test").build();
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
        @DisplayName("삭제된 멤버를 수정하면 예외가 발생한다.")
        void shouldThrowExceptionWhenMemberAlreadyDeleted() {
            // given
            Long deletedId = 1L;
            given(memberRepository.findById(deletedId))
                    .willReturn(Optional.of(deletedGoogleMember));

            // when & then
            assertThatThrownBy(() -> memberCommandService.update(deletedId, command))
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
            assertThat(result.profileImage()).isEqualTo(activeGoogleMember.getProfileImage());
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
            given(memberRepository.findById(unknownId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberCommandService.delete(unknownId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("삭제된 멤버를 삭제하면 예외가 발생한다.")
        void shouldThrowExceptionWhenMemberAlreadyDeleted() {
            // given
            Long deletedId = 1L;
            given(memberRepository.findById(deletedId))
                    .willReturn(Optional.of(deletedGoogleMember));

            // when & then
            assertThatThrownBy(() -> memberCommandService.delete(deletedId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(MemberErrorCode.MEMBER_ALREADY_WITHDRAWN.getMessage());
        }

        @Test
        @DisplayName("활성 멤버를 삭제하면 deletedAt이 설정된다.")
        void shouldSetDeletedAtWhenActiveMemberDeleted() {
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

    @Nested
    @DisplayName("(GOOGLE) restore 메서드는")
    class RestoreByGoogle {
        private MemberRestoreCommand command;

        @BeforeEach
        void setUp() {
            MemberRestoreCommandFixture commandFixture = new MemberRestoreCommandFixture(MemberFixture.GOOGLE);
            command = commandFixture.build();
        }

        @Test
        @DisplayName("멤버를 찾을 수 없으면 예외가 발생한다.")
        void shouldThrowExceptionWhenMemberNotFound() {
            // given
            given(memberRepository.findBySocialLogin(SocialProvider.GOOGLE, command.socialId()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberCommandService.restore(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("삭제되지 않은 멤버를 복구하면 예외가 발생한다.")
        void shouldThrowExceptionWhenMemberNotDeleted() {
            // given
            given(memberRepository.findBySocialLogin(SocialProvider.GOOGLE, command.socialId()))
                    .willReturn(Optional.of(activeGoogleMember));

            // when & then
            assertThatThrownBy(() -> memberCommandService.restore(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(MemberErrorCode.MEMBER_NOT_WITHDRAWN.getMessage());
        }

        @Test
        @DisplayName("삭제된 멤버를 복구하면 MemberResult를 반환한다.")
        void shouldReturnMemberResultWhenDeletedMemberRestored() {
            // given
            given(memberRepository.findBySocialLogin(SocialProvider.GOOGLE, command.socialId()))
                    .willReturn(Optional.of(deletedGoogleMember));

            // when
            MemberResult result = memberCommandService.restore(command);

            // then
            assertThat(deletedGoogleMember.isDeleted()).isFalse();
            assertThat(result.socialId()).isEqualTo(deletedGoogleMember.getSocialId());
            assertThat(result.socialProvider()).isEqualTo(deletedGoogleMember.getSocialProvider());
        }
    }

    @Nested
    @DisplayName("(APPLE) restore 메서드는")
    class RestoreByApple {
        private MemberRestoreCommand command;

        @BeforeEach
        void setUp() {
            MemberRestoreCommandFixture commandFixture = new MemberRestoreCommandFixture(MemberFixture.APPLE);
            command = commandFixture.build();
        }

        @Test
        @DisplayName("멤버를 찾을 수 없으면 예외가 발생한다.")
        void shouldThrowExceptionWhenMemberNotFound() {
            // given
            given(memberRepository.findBySocialLogin(SocialProvider.APPLE, command.socialId()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberCommandService.restore(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("삭제되지 않은 멤버를 복구하면 예외가 발생한다.")
        void shouldThrowExceptionWhenMemberNotDeleted() {
            // given
            given(memberRepository.findBySocialLogin(SocialProvider.APPLE, command.socialId()))
                    .willReturn(Optional.of(activeAppleMember));

            // when & then
            assertThatThrownBy(() -> memberCommandService.restore(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(MemberErrorCode.MEMBER_NOT_WITHDRAWN.getMessage());
        }

        @Test
        @DisplayName("삭제된 멤버를 복구하면 MemberResult를 반환한다.")
        void shouldReturnMemberResultWhenDeletedMemberRestored() {
            // given
            given(memberRepository.findBySocialLogin(SocialProvider.APPLE, command.socialId()))
                    .willReturn(Optional.of(deletedAppleMember));

            // when
            MemberResult result = memberCommandService.restore(command);

            // then
            assertThat(deletedAppleMember.isDeleted()).isFalse();
            assertThat(result.socialId()).isEqualTo(deletedAppleMember.getSocialId());
            assertThat(result.socialProvider()).isEqualTo(deletedAppleMember.getSocialProvider());
        }
    }
}
