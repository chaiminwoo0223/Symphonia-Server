package com.symphonia.member.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.symphonia.RepositoryTest;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.entity.Role;
import com.symphonia.member.domain.entity.SocialProvider;
import com.symphonia.member.domain.exception.MemberAlreadyExistsException;
import com.symphonia.member.domain.repository.MemberRepository;
import com.symphonia.member.fixture.MemberFixture;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@Import(MemberRepositoryImpl.class)
class MemberRepositoryImplTest extends RepositoryTest {

    @Autowired private MemberRepository memberRepository;

    @Nested
    @DisplayName("findBySocialLogin 메서드는")
    class FindBySocialLogin {

        @Nested
        @DisplayName("일치하는 회원이 있는 경우")
        class WhenMemberExists {

            @Test
            @DisplayName("회원을 반환한다")
            void shouldReturnMember() {
                // given
                Member saved = memberRepository.save(MemberFixture.KAKAO.create());

                // when
                Optional<Member> result =
                        memberRepository.findBySocialLogin(
                                SocialProvider.KAKAO, saved.getSocialId());

                // then
                assertThat(result).isPresent();
                assertThat(result.get().getId()).isEqualTo(saved.getId());
            }
        }

        @Nested
        @DisplayName("일치하는 회원이 없는 경우")
        class WhenMemberNotFound {

            @Test
            @DisplayName("빈 Optional을 반환한다")
            void shouldReturnEmpty() {
                // when
                Optional<Member> result =
                        memberRepository.findBySocialLogin(SocialProvider.KAKAO, "unknown");

                // then
                assertThat(result).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("findById 메서드는")
    class FindById {

        @Test
        @DisplayName("존재하는 ID면 회원을 반환한다")
        void shouldReturnMemberWhenIdExists() {
            // given
            Member saved = memberRepository.save(MemberFixture.GOOGLE.create());

            // when
            Optional<Member> result = memberRepository.findById(saved.getId());

            // then
            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("존재하지 않는 ID면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenIdNotExists() {
            // when
            Optional<Member> result = memberRepository.findById(-1L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Nested
        @DisplayName("저장된 적 없는 소셜 계정인 경우")
        class WhenSocialLoginIsNew {

            @Test
            @DisplayName("회원을 저장하고 ID를 채워 반환한다")
            void shouldPersistMember() {
                // given
                Member member = MemberFixture.KAKAO.create();

                // when
                Member saved = memberRepository.save(member);

                // then
                assertThat(saved.getId()).isNotNull();
            }
        }

        @Nested
        @DisplayName("같은 소셜 계정이 이미 저장되어 있는 경우")
        class WhenSocialLoginDuplicated {

            @Test
            @DisplayName("MemberAlreadyExistsException이 발생한다")
            void shouldThrowMemberAlreadyExistsException() {
                // given
                memberRepository.save(MemberFixture.KAKAO.create());

                // when & then
                assertThatThrownBy(() -> memberRepository.save(MemberFixture.KAKAO.create()))
                        .isInstanceOf(MemberAlreadyExistsException.class);
            }
        }

        @Nested
        @DisplayName("소셜 계정과 무관한 무결성 위반이 발생한 경우")
        class WhenOtherIntegrityViolationOccurs {

            @Test
            @DisplayName("변환하지 않고 DataIntegrityViolationException을 그대로 던진다")
            void shouldPropagateDataIntegrityViolationException() {
                // given
                Member memberWithoutNickname =
                        Member.builder()
                                .socialId(MemberFixture.KAKAO.getSocialId())
                                .email(MemberFixture.KAKAO.getEmail())
                                .role(Role.ROLE_MEMBER)
                                .socialProvider(SocialProvider.KAKAO)
                                .build();

                // when & then
                assertThatThrownBy(() -> memberRepository.save(memberWithoutNickname))
                        .isInstanceOf(DataIntegrityViolationException.class);
            }
        }

        @Nested
        @DisplayName("socialId만 같고 소셜 제공자가 다른 경우")
        class WhenOnlySocialIdMatches {

            @Test
            @DisplayName("회원을 저장한다")
            void shouldPersistMember() {
                // given
                memberRepository.save(MemberFixture.KAKAO.create());
                Member googleMember =
                        MemberFixture.GOOGLE.createWithSocialId(MemberFixture.KAKAO.getSocialId());

                // when
                Member saved = memberRepository.save(googleMember);

                // then
                assertThat(saved.getId()).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("existsBySocialLogin 메서드는")
    class ExistsBySocialLogin {

        @Test
        @DisplayName("일치하는 회원이 있으면 true를 반환한다")
        void shouldReturnTrueWhenMemberExists() {
            // given
            Member saved = memberRepository.save(MemberFixture.KAKAO.create());

            // when
            boolean result =
                    memberRepository.existsBySocialLogin(SocialProvider.KAKAO, saved.getSocialId());

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("일치하는 회원이 없으면 false를 반환한다")
        void shouldReturnFalseWhenMemberNotFound() {
            // when
            boolean result = memberRepository.existsBySocialLogin(SocialProvider.KAKAO, "unknown");

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("delete 메서드는")
    class Delete {

        @Test
        @DisplayName("회원을 삭제한다")
        void shouldRemoveMember() {
            // given
            Member saved = memberRepository.save(MemberFixture.KAKAO.create());

            // when
            memberRepository.delete(saved);

            // then
            assertThat(memberRepository.findById(saved.getId())).isEmpty();
        }
    }
}
