package com.symphonia.pairing.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.repository.AnjuRepository;
import com.symphonia.pairing.domain.vo.AllergyType;
import com.symphonia.pairing.fixture.AnjuFixture;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(AnjuRepositoryImpl.class)
class AnjuRepositoryImplTest extends RepositoryTest {

    @Autowired private AnjuRepository anjuRepository;
    @Autowired private AnjuJpaRepository anjuJpaRepository;

    @Nested
    @DisplayName("findAll 메서드는")
    class FindAll {

        @Test
        @DisplayName("저장된 모든 Anju를 반환한다")
        void shouldReturnAnjus() {
            // given
            AnjuJpaEntity saved =
                    anjuJpaRepository.save(
                            AnjuJpaEntity.from(AnjuFixture.GOLBAENGI_MUCHIM.create()));

            // when
            List<Anju> result = anjuRepository.findAll();

            // then
            assertThat(result).extracting(Anju::getId).contains(saved.getId());
        }
    }

    @Nested
    @DisplayName("findById 메서드는")
    class FindById {

        @Test
        @DisplayName("존재하는 ID면 Anju를 반환한다")
        void shouldReturnAnjuWhenIdExists() {
            // given
            AnjuJpaEntity saved =
                    anjuJpaRepository.save(
                            AnjuJpaEntity.from(AnjuFixture.GOLBAENGI_MUCHIM.create()));

            // when
            Optional<Anju> result = anjuRepository.findById(saved.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo(AnjuFixture.GOLBAENGI_MUCHIM.getName());
        }

        @Test
        @DisplayName("존재하지 않는 ID면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenIdNotExists() {
            // when
            Optional<Anju> result = anjuRepository.findById(-1L);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("allergyTypes 값을 포함해 Anju를 반환한다")
        void shouldReturnAnjuWithAllergyTypes() {
            // given
            AnjuJpaEntity saved =
                    anjuJpaRepository.save(AnjuJpaEntity.from(AnjuFixture.DRIED_SNACK.create()));

            // when
            Optional<Anju> result = anjuRepository.findById(saved.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getAllergyTypes())
                    .containsExactlyInAnyOrder(AllergyType.SQUID, AllergyType.PEANUT);
        }
    }
}
