package com.symphonia.catalog.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.symphonia.RepositoryTest;
import com.symphonia.catalog.domain.entity.DrinkStyle;
import com.symphonia.catalog.domain.repository.DrinkStyleRepository;
import com.symphonia.catalog.domain.vo.DrinkStyleSource;
import com.symphonia.catalog.fixture.DrinkStyleFixture;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

@Import(DrinkStyleRepositoryImpl.class)
@Sql("/sql/clear-catalog.sql")
class DrinkStyleRepositoryImplTest extends RepositoryTest {

    @Autowired private DrinkStyleRepository drinkStyleRepository;
    @Autowired private DrinkStyleJpaRepository drinkStyleJpaRepository;

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Test
        @DisplayName("DrinkStyle을 저장하고 ID를 채워 반환한다")
        void shouldPersistDrinkStyle() {
            // given
            DrinkStyle drinkStyle = DrinkStyleFixture.ALE.create();

            // when
            DrinkStyle saved = drinkStyleRepository.save(drinkStyle);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getName()).isEqualTo(DrinkStyleFixture.ALE.getName());
        }
    }

    @Nested
    @DisplayName("findById 메서드는")
    class FindById {

        @Test
        @DisplayName("존재하는 ID면 category와 flavorProfile을 포함해 DrinkStyle을 반환한다")
        void shouldReturnDrinkStyleWhenIdExists() {
            // given
            DrinkStyle saved = drinkStyleRepository.save(DrinkStyleFixture.PINOT_NOIR.create());

            // when
            Optional<DrinkStyle> result = drinkStyleRepository.findById(saved.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getCategory())
                    .isEqualTo(DrinkStyleFixture.PINOT_NOIR.getCategory());
            assertThat(result.get().getFlavorProfile().getAcidity())
                    .isEqualTo(DrinkStyleFixture.PINOT_NOIR.getFlavorProfile().getAcidity());
        }

        @Test
        @DisplayName("source, externalId 값을 포함해 DrinkStyle을 반환한다")
        void shouldReturnDrinkStyleWithSource() {
            // given
            DrinkStyle saved = drinkStyleRepository.save(DrinkStyleFixture.ALE.create());

            // when
            Optional<DrinkStyle> result = drinkStyleRepository.findById(saved.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getSource()).isEqualTo(DrinkStyleSource.SEED);
            assertThat(result.get().getExternalId())
                    .isEqualTo(DrinkStyleFixture.ALE.getExternalId());
        }

        @Test
        @DisplayName("존재하지 않는 ID면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenIdNotExists() {
            // when
            Optional<DrinkStyle> result = drinkStyleRepository.findById(-1L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllBySource 메서드는")
    class FindAllBySource {

        @Test
        @DisplayName("source가 일치하는 DrinkStyle을 반환한다")
        void shouldReturnDrinkStylesWhenSourceMatches() {
            // given
            DrinkStyle saved = drinkStyleRepository.save(DrinkStyleFixture.ALE.create());

            // when
            List<DrinkStyle> result = drinkStyleRepository.findAllBySource(DrinkStyleSource.SEED);

            // then
            assertThat(result)
                    .filteredOn(drinkStyle -> drinkStyle.getId().equals(saved.getId()))
                    .singleElement()
                    .extracting(DrinkStyle::getExternalId)
                    .isEqualTo(DrinkStyleFixture.ALE.getExternalId());
        }

        @Test
        @DisplayName("source가 다른 DrinkStyle은 반환하지 않는다")
        void shouldExcludeDrinkStylesWhenSourceDiffers() {
            // given
            DrinkStyle saved = drinkStyleRepository.save(DrinkStyleFixture.ALE.create());

            // when
            List<DrinkStyle> result = drinkStyleRepository.findAllBySource(DrinkStyleSource.BJCP);

            // then
            assertThat(result).extracting(DrinkStyle::getId).doesNotContain(saved.getId());
        }
    }

    @Nested
    @DisplayName("saveAndFlush 메서드는")
    class SaveAndFlush {

        @Test
        @DisplayName("같은 source와 externalId를 가진 DrinkStyle을 두 번 저장할 수 없다")
        void shouldThrowDataIntegrityViolationExceptionWhenSourceAndExternalIdDuplicated() {
            // given
            drinkStyleJpaRepository.saveAndFlush(
                    DrinkStyleJpaEntity.from(DrinkStyleFixture.ALE.create()));
            // name이 아니라 (source, external_id) 유니크 제약에 걸리도록 이름이 다른 스타일에 같은 키를 준다.
            DrinkStyle duplicated =
                    DrinkStyleFixture.BOURBON.createWithIdentity(
                            DrinkStyleSource.SEED, DrinkStyleFixture.ALE.getExternalId());

            // when & then
            assertThatThrownBy(
                            () ->
                                    drinkStyleJpaRepository.saveAndFlush(
                                            DrinkStyleJpaEntity.from(duplicated)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}
