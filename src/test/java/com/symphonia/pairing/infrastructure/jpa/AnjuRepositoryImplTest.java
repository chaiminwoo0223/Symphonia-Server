package com.symphonia.pairing.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.repository.AnjuRepository;
import com.symphonia.pairing.domain.vo.AllergyType;
import com.symphonia.pairing.domain.vo.AnjuSource;
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
import org.springframework.test.context.jdbc.Sql;

@Import(AnjuRepositoryImpl.class)
@Sql("/sql/clear-pairing-catalog.sql")
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
            Statistics statistics = enabledStatistics();

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

        @Test
        @DisplayName("주재료 계열, 형태, 매운맛 여부를 포함해 Anju를 반환한다")
        void shouldReturnAnjuWithClassification() {
            // given
            AnjuJpaEntity saved =
                    anjuJpaRepository.save(
                            AnjuJpaEntity.from(AnjuFixture.GOLBAENGI_MUCHIM.create()));
            entityManager.flush();
            entityManager.clear();

            // when
            Optional<Anju> result = anjuRepository.findById(saved.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getBase()).isEqualTo(AnjuFixture.GOLBAENGI_MUCHIM.getBase());
            assertThat(result.get().getServing())
                    .isEqualTo(AnjuFixture.GOLBAENGI_MUCHIM.getServing());
            assertThat(result.get().isSpicy()).isEqualTo(AnjuFixture.GOLBAENGI_MUCHIM.isSpicy());
        }

        @Test
        @DisplayName("flavorProfile의 acidity 값을 포함해 Anju를 반환한다")
        void shouldReturnAnjuWithAcidity() {
            // given
            AnjuJpaEntity saved =
                    anjuJpaRepository.save(
                            AnjuJpaEntity.from(AnjuFixture.GOLBAENGI_MUCHIM.create()));

            // when
            Optional<Anju> result = anjuRepository.findById(saved.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getFlavorProfile().getAcidity())
                    .isEqualTo(AnjuFixture.GOLBAENGI_MUCHIM.getFlavorProfile().getAcidity());
        }
    }

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Test
        @DisplayName("id가 없는 Anju는 새로 저장하고 id를 채워 반환한다")
        void shouldInsertAnjuWhenIdMissing() {
            // given
            Anju anju = AnjuFixture.FRIED_CHICKEN.createWithIdentity(AnjuSource.K_FIND, "D101");

            // when
            Anju saved = anjuRepository.save(anju);

            // then
            entityManager.flush();
            entityManager.clear();
            AnjuJpaEntity found = anjuJpaRepository.findById(saved.getId()).orElseThrow();
            assertThat(found.getSource()).isEqualTo(AnjuSource.K_FIND);
            assertThat(found.getExternalId()).isEqualTo("D101");
            assertThat(found.getAllergyTypes())
                    .containsExactlyInAnyOrderElementsOf(
                            AnjuFixture.FRIED_CHICKEN.getAllergyTypes());
        }

        @Test
        @DisplayName("id가 있는 Anju는 같은 행의 맛과 알레르기를 갱신한다")
        void shouldUpdateAnjuWhenIdExists() {
            // given
            Anju existing =
                    anjuRepository.save(
                            AnjuFixture.FRIED_CHICKEN.createWithIdentity(
                                    AnjuSource.K_FIND, "D101"));
            entityManager.flush();
            entityManager.clear();
            AnjuFixture imported = AnjuFixture.GOLBAENGI_MUCHIM;
            Anju updated =
                    existing.overwriteWith(imported.createWithIdentity(AnjuSource.K_FIND, "D101"));

            // when
            anjuRepository.save(updated);

            // then
            entityManager.flush();
            entityManager.clear();
            AnjuJpaEntity found = anjuJpaRepository.findById(existing.getId()).orElseThrow();
            assertThat(anjuJpaRepository.count()).isEqualTo(1);
            assertThat(found.getName()).isEqualTo(imported.getName());
            assertThat(found.getFlavorProfile().toDomain())
                    .usingRecursiveComparison()
                    .isEqualTo(imported.getFlavorProfile());
            assertThat(found.getAllergyTypes())
                    .containsExactlyInAnyOrderElementsOf(imported.getAllergyTypes());
            assertThat(found.getBase()).isEqualTo(imported.getBase());
            assertThat(found.isSpicy()).isEqualTo(imported.isSpicy());
        }

        // 적재 서비스가 기존 안주를 findAll로 한 번에 불러온 뒤 행마다 저장할 때 N+1이 생기지 않는지 검증한다.
        @Test
        @DisplayName("findAll로 불러온 Anju를 갱신해 저장하면 SELECT를 다시 실행하지 않는다")
        void shouldNotSelectAgainWhenSavingAnjuLoadedByFindAll() {
            // given
            anjuJpaRepository.save(AnjuJpaEntity.from(AnjuFixture.DRIED_SNACK.create()));
            entityManager.flush();
            entityManager.clear();
            Anju loaded = anjuRepository.findAll().getFirst();
            Anju updated =
                    loaded.overwriteWith(
                            AnjuFixture.FRIED_CHICKEN.createWithIdentity(
                                    loaded.getSource(), loaded.getExternalId()));
            Statistics statistics = enabledStatistics();

            // when
            anjuRepository.save(updated);

            // then
            assertThat(statistics.getPrepareStatementCount()).isZero();
        }
    }

    private Statistics enabledStatistics() {
        Statistics statistics =
                entityManager
                        .getEntityManagerFactory()
                        .unwrap(SessionFactory.class)
                        .getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        return statistics;
    }
}
