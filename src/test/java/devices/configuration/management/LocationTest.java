package devices.configuration.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocationTest {

    @Test
    void shouldCreateLocationWithAllFields() {
        // Given
        Coordinates coordinates = Coordinates.of(16.931752852309156, 51.09836221719513);

        // When
        Location location = Location.of(
                "Rakietowa",
                "1A",
                "Wrocław",
                "54-621",
                null,
                "POL",
                coordinates
        );

        // Then
        assertThat(location.street()).isEqualTo("Rakietowa");
        assertThat(location.houseNumber()).isEqualTo("1A");
        assertThat(location.city()).isEqualTo("Wrocław");
        assertThat(location.postalCode()).isEqualTo("54-621");
        assertThat(location.state()).isNull();
        assertThat(location.country()).isEqualTo("POL");
        assertThat(location.coordinates()).isEqualTo(coordinates);
    }

    @Test
    void shouldUseValueEquality() {
        // Given
        Coordinates coordinates = Coordinates.of(16.931752852309156, 51.09836221719513);
        Location location1 = Location.of("Rakietowa", "1A", "Wrocław", "54-621", null, "POL", coordinates);
        Location location2 = Location.of("Rakietowa", "1A", "Wrocław", "54-621", null, "POL", coordinates);

        // When & Then
        assertThat(location1).isEqualTo(location2);
    }

    @Test
    void shouldFailWhenCoordinatesAreMissing() {
        // When & Then
        assertThatThrownBy(() -> Location.of("Rakietowa", "1A", "Wrocław", "54-621", null, "POL", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
