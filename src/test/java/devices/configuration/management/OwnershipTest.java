package devices.configuration.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnershipTest {

    @Test
    void shouldCreateUnownedOwnership() {
        // Given & When
        Ownership ownership = Ownership.unowned();

        // Then
        assertThat(ownership.isUnowned()).isTrue();
        assertThat(ownership.isOwned()).isFalse();
        assertThat(ownership.operator()).isNull();
        assertThat(ownership.provider()).isNull();
    }

    @Test
    void shouldCreateOwnedOwnership() {
        // Given & When
        Ownership ownership = Ownership.of("Devicex.nl", "public-devices");

        // Then
        assertThat(ownership.isOwned()).isTrue();
        assertThat(ownership.isUnowned()).isFalse();
        assertThat(ownership.operator()).isEqualTo("Devicex.nl");
        assertThat(ownership.provider()).isEqualTo("public-devices");
    }

    @Test
    void shouldUseValueEquality() {
        // Given
        Ownership ownership1 = Ownership.of("Devicex.nl", "public-devices");
        Ownership ownership2 = Ownership.of("Devicex.nl", "public-devices");

        // When & Then
        assertThat(ownership1).isEqualTo(ownership2);
    }

    @Test
    void shouldFailWhenOnlyOperatorIsSet() {
        // When & Then
        assertThatThrownBy(() -> new Ownership("Devicex.nl", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFailWhenOnlyProviderIsSet() {
        // When & Then
        assertThatThrownBy(() -> new Ownership(null, "public-devices"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
