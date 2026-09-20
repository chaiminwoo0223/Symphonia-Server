package com.symphonia.pairing.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.repository.AnjuRepository;
import com.symphonia.pairing.domain.vo.AllergyType;
import com.symphonia.pairing.fixture.AnjuFixture;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(AnjuRepositoryImpl.class)
class AnjuRepositoryImplTest extends RepositoryTest {

    @Autowired private AnjuRepository anjuRepository;
    @Autowired private AnjuJpaRepository anjuJpaRepository;
    @Autowired private EntityManager entityManager;

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

        @Test
        @DisplayName("알레르기가 없는 Anju도 allergyTypes가 빈 Set인 채로 반환한다")
        void shouldReturnAnjuWithoutAllergyTypes() {
            // given
            AnjuJpaEntity saved =
                    anjuJpaRepository.save(AnjuJpaEntity.from(AnjuFixture.FRUIT_PLATTER.create()));
            entityManager.flush();
            entityManager.clear();

            // when
            List<Anju> result = anjuRepository.findAll();

            // then
            assertThat(result)
                    .filteredOn(anju -> anju.getId().equals(saved.getId()))
                    .singleElement()
                    .satisfies(anju -> assertThat(anju.getAllergyTypes()).isEmpty());
        }

        @Test
        @DisplayName("Anju 개수와 무관하게 쿼리를 한 번만 실행한다")
        void shouldExecuteSingleQueryRegardlessOfAnjuCount() {
            // given
            anjuJpaRepository.save(AnjuJpaEntity.from(AnjuFixture.GOLBAENGI_MUCHIM.create()));
            anjuJpaRepository.save(AnjuJpaEntity.from(AnjuFixture.FRIED_CHICKEN.create()));
            anjuJpaRepository.save(AnjuJpaEntity.from(AnjuFixture.DRIED_SNACK.create()));
            entityManager.flush();
            entityManager.clear();
            Statistics statistics =
                    entityManager
                            .getEntityManagerFactory()
                            .unwrap(SessionFactory.class)
                            .getStatistics();
            statistics.setStatisticsEnabled(true);
            statistics.clear();

            // when
            List<Anju> result = anjuRepository.findAll();

            // then
            assertThat(result).hasSizeGreaterThanOrEqualTo(3);
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
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
        @DisplayName("영속성 컨텍스트가 정리된 뒤에도 allergyTypes를 읽을 수 있다")
        void shouldReadAllergyTypesAfterPersistenceContextCleared() {
            // given
            AnjuJpaEntity saved =
                    anjuJpaRepository.save(AnjuJpaEntity.from(AnjuFixture.DRIED_SNACK.create()));
            entityManager.flush();
            entityManager.clear();

            // when
            Anju anju = anjuRepository.findById(saved.getId()).orElseThrow();
            entityManager.clear();

            // then
            assertThat(anju.getAllergyTypes())
                    .containsExactlyInAnyOrder(AllergyType.SQUID, AllergyType.PEANUT);
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
