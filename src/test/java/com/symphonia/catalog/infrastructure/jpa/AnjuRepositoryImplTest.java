package com.symphonia.catalog.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.catalog.domain.entity.Anju;
import com.symphonia.catalog.domain.repository.AnjuRepository;
import com.symphonia.catalog.domain.vo.AllergyType;
import com.symphonia.catalog.domain.vo.AnjuSource;
import com.symphonia.catalog.fixture.AnjuFixture;
import com.symphonia.catalog.fixture.DrinkFixture;
import com.symphonia.catalog.fixture.DrinkStyleFixture;
import com.symphonia.catalog.fixture.MusicMoodFixture;
import com.symphonia.pairing.fixture.PairingFeedbackFixture;
import com.symphonia.pairing.infrastructure.jpa.PairingFeedbackJpaEntity;
import com.symphonia.pairing.infrastructure.jpa.PairingFeedbackJpaRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Import(AnjuRepositoryImpl.class)
@Sql("/sql/clear-catalog.sql")
class AnjuRepositoryImplTest extends RepositoryTest {

    @Autowired private AnjuRepository anjuRepository;
    @Autowired private AnjuJpaRepository anjuJpaRepository;
    @Autowired private DrinkStyleJpaRepository drinkStyleJpaRepository;
    @Autowired private DrinkJpaRepository drinkJpaRepository;
    @Autowired private MusicMoodJpaRepository musicMoodJpaRepository;
    @Autowired private PairingFeedbackJpaRepository pairingFeedbackJpaRepository;
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
    @DisplayName("deleteAllById 메서드는")
    class DeleteAllById {

        @Test
        @DisplayName("안주와 그 알레르기 행을 함께 지운다")
        void shouldDeleteAnjuWithAllergyTypes() {
            // given
            Long anjuId =
                    anjuJpaRepository
                            .save(AnjuJpaEntity.from(AnjuFixture.DRIED_SNACK.create()))
                            .getId();

            // when
            anjuRepository.deleteAllById(List.of(anjuId));

            // then
            assertThat(anjuJpaRepository.findById(anjuId)).isEmpty();
            assertThat(countAllergyTypeRowsOf(anjuId)).isZero();
        }

        // 적재 서비스는 매핑표에서 빠진 안주를 지운 뒤 같은 이름의 안주를 다른 식별 키로 저장한다.
        // Hibernate는 같은 트랜잭션의 INSERT를 DELETE보다 먼저 실행하므로, 지우기가 바로 반영돼야 uk_anju_name에 걸리지 않는다.
        @Test
        @DisplayName("flush를 기다리지 않고 DELETE를 바로 실행한다")
        void shouldExecuteDeleteImmediately() {
            // given
            Long anjuId =
                    anjuJpaRepository
                            .save(AnjuJpaEntity.from(AnjuFixture.DRIED_SNACK.create()))
                            .getId();
            entityManager.flush();
            entityManager.clear();
            Statistics statistics = enabledStatistics();

            // when
            anjuRepository.deleteAllById(List.of(anjuId));

            // then
            assertThat(statistics.getEntityDeleteCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("findReferencedIds 메서드는")
    class FindReferencedIds {

        @Test
        @DisplayName("주어진 안주 중 피드백이 참조하는 안주의 id만 반환한다")
        void shouldReturnOnlyReferencedAnjuIds() {
            // given
            Long referencedAnjuId = saveAnju(AnjuFixture.GOLBAENGI_MUCHIM);
            Long unreferencedAnjuId = saveAnju(AnjuFixture.FRIED_CHICKEN);
            saveFeedbackOn(referencedAnjuId);

            // when
            Set<Long> result =
                    anjuRepository.findReferencedIds(List.of(referencedAnjuId, unreferencedAnjuId));

            // then
            assertThat(result).containsExactly(referencedAnjuId);
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

    private Long saveAnju(AnjuFixture anjuFixture) {
        return anjuJpaRepository.save(AnjuJpaEntity.from(anjuFixture.create())).getId();
    }

    private void saveFeedbackOn(Long anjuId) {
        Long drinkStyleId =
                drinkStyleJpaRepository
                        .save(DrinkStyleJpaEntity.from(DrinkStyleFixture.DISTILLED_SOJU.create()))
                        .getId();
        Long drinkId =
                drinkJpaRepository
                        .save(DrinkJpaEntity.from(DrinkFixture.SOJU.createWithStyle(drinkStyleId)))
                        .getId();
        Long musicMoodId =
                musicMoodJpaRepository
                        .save(MusicMoodJpaEntity.from(MusicMoodFixture.FORMAL_JAZZ.create()))
                        .getId();
        pairingFeedbackJpaRepository.save(
                PairingFeedbackJpaEntity.from(
                        PairingFeedbackFixture.FRIEND_FORMAL.createWithReferences(
                                drinkId, anjuId, musicMoodId)));
    }

    private long countAllergyTypeRowsOf(Long anjuId) {
        return ((Number)
                        entityManager
                                .createNativeQuery(
                                        "select count(*) from anju_allergy_type where anju_id = :anjuId")
                                .setParameter("anjuId", anjuId)
                                .getSingleResult())
                .longValue();
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
