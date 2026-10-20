package com.symphonia.pairing.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.symphonia.RepositoryTest;
import com.symphonia.pairing.domain.vo.AnjuBase;
import com.symphonia.pairing.domain.vo.AnjuSource;
import com.symphonia.pairing.domain.vo.DrinkSource;
import com.symphonia.pairing.domain.vo.DrinkStyleSource;
import com.symphonia.pairing.domain.vo.Harmonize;
import com.symphonia.pairing.domain.vo.ServingStyle;
import com.symphonia.pairing.fixture.AnjuFixture;
import com.symphonia.pairing.fixture.DrinkFixture;
import com.symphonia.pairing.fixture.DrinkStyleFixture;
import com.symphonia.pairing.fixture.MusicMoodFixture;
import com.symphonia.pairing.fixture.XWineFixture;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PairingSeedMigrationTest extends RepositoryTest {

    @Autowired private DrinkStyleJpaRepository drinkStyleJpaRepository;
    @Autowired private DrinkJpaRepository drinkJpaRepository;
    @Autowired private AnjuJpaRepository anjuJpaRepository;
    @Autowired private MusicMoodJpaRepository musicMoodJpaRepository;

    // 시드 마이그레이션에서 setval 보정이 빠지면 IDENTITY가 시드 id를 다시 발급해 save()가 중복 키로 실패한다.
    @Test
    @DisplayName("시드 이후 저장해도 ID가 충돌하지 않는다")
    void shouldSaveWithoutIdCollision() {
        // given: 시드 마이그레이션이 적재된 상태

        // when
        DrinkStyleJpaEntity drinkStyle =
                drinkStyleJpaRepository.save(
                        DrinkStyleJpaEntity.from(DrinkStyleFixture.DISTILLED_SOJU.create()));
        DrinkJpaEntity drink =
                drinkJpaRepository.save(
                        DrinkJpaEntity.from(DrinkFixture.SOJU.createWithStyle(drinkStyle.getId())));
        AnjuJpaEntity anju =
                anjuJpaRepository.save(AnjuJpaEntity.from(AnjuFixture.BOILED_PORK.create()));
        MusicMoodJpaEntity musicMood =
                musicMoodJpaRepository.save(
                        MusicMoodJpaEntity.from(MusicMoodFixture.CASUAL_LOFI.create()));

        // then
        assertThat(drinkStyle.getId()).isNotNull();
        assertThat(drink.getId()).isNotNull();
        assertThat(anju.getId()).isNotNull();
        assertThat(musicMood.getId()).isNotNull();
    }

    // V6가 레드 와인 시드를 지우고 다시 넣으면 id가 바뀌어 pairing_feedback의 FK가 끊긴다.
    @Test
    @DisplayName("시드 레드 와인은 같은 id를 유지한 채 X-Wines의 Louis Jadot Beaujolais로 바뀐다")
    void shouldReplaceSeedRedWineWithXWinesProductKeepingId() {
        // given: 시드 마이그레이션이 적재된 상태

        // when
        Optional<DrinkJpaEntity> redWine = drinkJpaRepository.findById(4L);

        // then
        assertThat(redWine).isPresent();
        assertThat(redWine.get().getSource()).isEqualTo(DrinkSource.X_WINES);
        assertThat(redWine.get().getExternalId()).isEqualTo("111692");
        assertThat(redWine.get().getName()).isEqualTo("Louis Jadot Beaujolais");
        assertThat(redWine.get().getAbv()).isEqualTo(12.5);
        assertThat(drinkJpaRepository.findAllBySource(DrinkSource.SEED))
                .extracting(DrinkJpaEntity::getExternalId)
                .doesNotContain("red-wine");
    }

    // 시드 소주는 적재 러너가 다루지 않으므로 음식 계열이 빠지면 Harmonize 점수를 받지 못한다.
    @Test
    @DisplayName("시드 소주에는 돼지고기, 소고기 음식 계열이 채워진다")
    void shouldFillSeedSojuHarmonizes() {
        // given: 시드 마이그레이션이 적재된 상태

        // when
        List<DrinkJpaEntity> seedDrinks = drinkJpaRepository.findAllBySource(DrinkSource.SEED);

        // then
        assertThat(seedDrinks)
                .filteredOn(drink -> drink.getExternalId().equals("soju"))
                .singleElement()
                .extracting(DrinkJpaEntity::getHarmonizes)
                .isEqualTo(Set.of(Harmonize.PORK, Harmonize.BEEF));
    }

    // check 제약에 X_WINES가 빠지면 와인 적재의 첫 INSERT가 제약 위반으로 실패한다.
    @Test
    @DisplayName("출처가 X_WINES인 스타일과 음료를 저장할 수 있다")
    void shouldSaveXWinesStyleAndDrink() {
        // given
        XWineFixture wineFixture = XWineFixture.ORIGEM_MERLOT;

        // when
        DrinkStyleJpaEntity wineStyle =
                drinkStyleJpaRepository.saveAndFlush(
                        DrinkStyleJpaEntity.from(
                                DrinkStyleFixture.PINOT_NOIR.createWithIdentity(
                                        DrinkStyleSource.X_WINES, "fixture-red")));
        DrinkJpaEntity wine =
                drinkJpaRepository.saveAndFlush(
                        DrinkJpaEntity.from(
                                wineFixture.createDrink(
                                        wineStyle.getId(),
                                        DrinkStyleFixture.PINOT_NOIR.getFlavorProfile())));

        // then
        assertThat(wineStyle.getSource()).isEqualTo(DrinkStyleSource.X_WINES);
        assertThat(wine.getSource()).isEqualTo(DrinkSource.X_WINES);
    }

    // V7이 시드 안주를 지우고 다시 넣으면 id가 바뀌어 pairing_feedback의 FK가 끊긴다.
    @Test
    @DisplayName("시드 안주 6행은 같은 id를 유지한 채 출처 식별 키와 주재료 계열, 형태, 매운맛 여부가 채워진다")
    void shouldFillSeedAnjuClassificationKeepingId() {
        // given: 시드 마이그레이션이 적재된 상태

        // when
        List<AnjuJpaEntity> seedAnjus =
                anjuJpaRepository.findAllById(List.of(1L, 2L, 3L, 4L, 5L, 6L));

        // then
        assertThat(seedAnjus)
                .extracting(
                        AnjuJpaEntity::getId,
                        AnjuJpaEntity::getSource,
                        AnjuJpaEntity::getExternalId,
                        AnjuJpaEntity::getBase,
                        AnjuJpaEntity::getServing,
                        AnjuJpaEntity::isSpicy)
                .containsExactlyInAnyOrder(
                        tuple(
                                1L,
                                AnjuSource.SEED,
                                "golbaengi-muchim",
                                AnjuBase.SEAFOOD,
                                ServingStyle.MAIN,
                                true),
                        tuple(
                                2L,
                                AnjuSource.SEED,
                                "fried-chicken",
                                AnjuBase.POULTRY,
                                ServingStyle.MAIN,
                                false),
                        tuple(
                                3L,
                                AnjuSource.SEED,
                                "tofu-kimchi",
                                AnjuBase.PORK,
                                ServingStyle.MAIN,
                                true),
                        tuple(
                                4L,
                                AnjuSource.SEED,
                                "dried-snack",
                                AnjuBase.SEAFOOD,
                                ServingStyle.SNACK,
                                false),
                        tuple(
                                5L,
                                AnjuSource.SEED,
                                "cheese-platter",
                                AnjuBase.HARD_CHEESE,
                                ServingStyle.APPETIZER,
                                false),
                        tuple(
                                6L,
                                AnjuSource.SEED,
                                "fruit-platter",
                                AnjuBase.SWEET,
                                ServingStyle.SNACK,
                                false));
    }

    // check 제약에 K_FIND가 빠지면 안주 적재의 첫 INSERT가 제약 위반으로 실패한다.
    @Test
    @DisplayName("출처가 K_FIND인 안주를 저장할 수 있다")
    void shouldSaveKFindAnju() {
        // given
        AnjuJpaEntity kFindAnju =
                AnjuJpaEntity.from(
                        AnjuFixture.FRIED_SHRIMP.createWithIdentity(
                                AnjuSource.K_FIND, "fixture-k-find-code"));

        // when
        AnjuJpaEntity anju = anjuJpaRepository.saveAndFlush(kFindAnju);

        // then
        assertThat(anju.getSource()).isEqualTo(AnjuSource.K_FIND);
    }
}
