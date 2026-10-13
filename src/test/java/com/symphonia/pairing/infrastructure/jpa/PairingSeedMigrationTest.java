package com.symphonia.pairing.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.vo.DrinkSource;
import com.symphonia.pairing.domain.vo.DrinkStyleSource;
import com.symphonia.pairing.fixture.AnjuFixture;
import com.symphonia.pairing.fixture.DrinkFixture;
import com.symphonia.pairing.fixture.DrinkStyleFixture;
import com.symphonia.pairing.fixture.MusicMoodFixture;
import com.symphonia.pairing.fixture.XWineFixture;
import java.util.Optional;
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
        Optional<DrinkJpaEntity> redWine =
                drinkJpaRepository.findBySourceAndExternalId(DrinkSource.X_WINES, "111692");

        // then
        assertThat(redWine).isPresent();
        assertThat(redWine.get().getId()).isEqualTo(4L);
        assertThat(redWine.get().getName()).isEqualTo("Louis Jadot Beaujolais");
        assertThat(redWine.get().getAbv()).isEqualTo(12.5);
        assertThat(drinkJpaRepository.findBySourceAndExternalId(DrinkSource.SEED, "red-wine"))
                .isEmpty();
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
                                Drink.of(
                                        wineStyle.getId(),
                                        DrinkSource.X_WINES,
                                        wineFixture.getWineId(),
                                        wineFixture.getDisplayName(),
                                        wineFixture.getAbv(),
                                        DrinkStyleFixture.PINOT_NOIR.getFlavorProfile(),
                                        false)));

        // then
        assertThat(wineStyle.getSource()).isEqualTo(DrinkStyleSource.X_WINES);
        assertThat(wine.getSource()).isEqualTo(DrinkSource.X_WINES);
    }
}
