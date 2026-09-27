package com.symphonia.pairing.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.pairing.domain.entity.DrinkStyle;
import com.symphonia.pairing.domain.repository.DrinkStyleRepository;
import com.symphonia.pairing.fixture.DrinkStyleFixture;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Import(DrinkStyleRepositoryImpl.class)
@Sql("/sql/clear-pairing-catalog.sql")
class DrinkStyleRepositoryImplTest extends RepositoryTest {

    @Autowired private DrinkStyleRepository drinkStyleRepository;

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
        @DisplayName("존재하지 않는 ID면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenIdNotExists() {
            // when
            Optional<DrinkStyle> result = drinkStyleRepository.findById(-1L);

            // then
            assertThat(result).isEmpty();
        }
    }
}
