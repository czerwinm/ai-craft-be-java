package devices.configuration.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceConfigurationTest {

    @Test
    void shouldCreateNewDeviceWithDefaultConfiguration() {
        // Given & When
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");

        // Then
        DeviceConfigurationSnapshot snapshot = device.toSnapshot();
        assertThat(snapshot.deviceId()).isEqualTo("ALF-98262561");
        assertThat(snapshot.ownership()).isEqualTo(Ownership.unowned());
        assertThat(snapshot.location()).isNull();
        assertThat(snapshot.openingHours()).isEqualTo(OpeningHours.alwaysOpened());
        assertThat(snapshot.settings()).isEqualTo(Settings.defaultSettings());
        assertThat(device.events).hasSize(1);
        assertThat(device.events.get(0)).isInstanceOf(DomainEvent.DeviceCreated.class);
    }

    @Test
    void shouldChangeOwnershipAndEmitEvent() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");
        device.events.clear();

        // When
        device.changeOwnership(Ownership.of("Devicex.nl", "public-devices"));

        // Then
        assertThat(device.toSnapshot().ownership()).isEqualTo(Ownership.of("Devicex.nl", "public-devices"));
        assertThat(device.events).hasSize(1);
        assertThat(device.events.get(0)).isInstanceOf(DomainEvent.OwnershipChanged.class);
        DomainEvent.OwnershipChanged event = (DomainEvent.OwnershipChanged) device.events.get(0);
        assertThat(event.deviceId()).isEqualTo("ALF-98262561");
        assertThat(event.ownership()).isEqualTo(Ownership.of("Devicex.nl", "public-devices"));
    }

    @Test
    void shouldNotEmitEventWhenOwnershipUnchanged() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");
        device.changeOwnership(Ownership.of("Devicex.nl", "public-devices"));
        device.events.clear();

        // When
        device.changeOwnership(Ownership.of("Devicex.nl", "public-devices"));

        // Then
        assertThat(device.events).isEmpty();
    }

    @Test
    void shouldResetToDefaultsWhenSettingOwnershipToUnowned() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");
        device.changeOwnership(Ownership.of("Devicex.nl", "public-devices"));
        device.changeLocation(Location.of(
                "Rakietowa", "1A", "Wrocław", "54-621", null, "POL",
                Coordinates.of(16.931752852309156, 51.09836221719513)
        ));
        device.changeSettings(Settings.of(true, true, true, true, true, true));
        device.events.clear();

        // When
        device.changeOwnership(Ownership.unowned());

        // Then
        DeviceConfigurationSnapshot snapshot = device.toSnapshot();
        assertThat(snapshot.ownership()).isEqualTo(Ownership.unowned());
        assertThat(snapshot.location()).isNull();
        assertThat(snapshot.openingHours()).isEqualTo(OpeningHours.alwaysOpened());
        assertThat(snapshot.settings()).isEqualTo(Settings.defaultSettings());
        assertThat(device.events).hasSizeGreaterThan(1);
        assertThat(device.events.get(0)).isInstanceOf(DomainEvent.OwnershipChanged.class);
    }

    @Test
    void shouldChangeLocationAndEmitEvent() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");
        device.events.clear();
        Location location = Location.of(
                "Rakietowa", "1A", "Wrocław", "54-621", null, "POL",
                Coordinates.of(16.931752852309156, 51.09836221719513)
        );

        // When
        device.changeLocation(location);

        // Then
        assertThat(device.toSnapshot().location()).isEqualTo(location);
        assertThat(device.events).hasSize(2);
        assertThat(device.events.get(0)).isInstanceOf(DomainEvent.LocationChanged.class);
        assertThat(device.events.get(1)).isInstanceOf(DomainEvent.DeviceConfigurationChanged.class);
    }

    @Test
    void shouldNotEmitEventWhenLocationUnchanged() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");
        Location location = Location.of(
                "Rakietowa", "1A", "Wrocław", "54-621", null, "POL",
                Coordinates.of(16.931752852309156, 51.09836221719513)
        );
        device.changeLocation(location);
        device.events.clear();

        // When
        device.changeLocation(location);

        // Then
        assertThat(device.events).isEmpty();
    }

    @Test
    void shouldChangeOpeningHoursAndEmitEvent() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");
        device.events.clear();
        OpeningHours openingHours = OpeningHours.of(false);

        // When
        device.changeOpeningHours(openingHours);

        // Then
        assertThat(device.toSnapshot().openingHours()).isEqualTo(openingHours);
        assertThat(device.events).hasSize(2);
        assertThat(device.events.get(0)).isInstanceOf(DomainEvent.OpeningHoursChanged.class);
        assertThat(device.events.get(1)).isInstanceOf(DomainEvent.DeviceConfigurationChanged.class);
    }

    @Test
    void shouldNotEmitEventWhenOpeningHoursUnchanged() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");
        device.events.clear();

        // When
        device.changeOpeningHours(OpeningHours.alwaysOpened());

        // Then
        assertThat(device.events).isEmpty();
    }

    @Test
    void shouldChangeSettingsAndEmitEvent() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");
        device.events.clear();
        Settings settings = Settings.of(true, false, true, false, true, false);

        // When
        device.changeSettings(settings);

        // Then
        assertThat(device.toSnapshot().settings()).isEqualTo(settings);
        assertThat(device.events).hasSize(2);
        assertThat(device.events.get(0)).isInstanceOf(DomainEvent.SettingsChanged.class);
        assertThat(device.events.get(1)).isInstanceOf(DomainEvent.DeviceConfigurationChanged.class);
    }

    @Test
    void shouldNotEmitEventWhenSettingsUnchanged() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");
        device.events.clear();

        // When
        device.changeSettings(Settings.defaultSettings());

        // Then
        assertThat(device.events).isEmpty();
    }

    @Test
    void shouldFailWhenOwnershipIsNull() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");

        // When & Then
        assertThatThrownBy(() -> device.changeOwnership(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Ownership cannot be null");
    }

    @Test
    void shouldFailWhenOpeningHoursIsNull() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");

        // When & Then
        assertThatThrownBy(() -> device.changeOpeningHours(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("OpeningHours cannot be null");
    }

    @Test
    void shouldFailWhenSettingsIsNull() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");

        // When & Then
        assertThatThrownBy(() -> device.changeSettings(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Settings cannot be null");
    }

    @Test
    void shouldAllowNullLocation() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");

        // When
        device.changeLocation(null);

        // Then
        assertThat(device.toSnapshot().location()).isNull();
    }

    @Test
    void shouldDetectOperatorNotAssigned() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");
        // Device starts with unowned ownership

        // When
        Violations violations = device.toSnapshot().violations();

        // Then
        assertThat(violations.operatorNotAssigned()).isTrue();
        assertThat(violations.isValid()).isFalse();
    }

    @Test
    void shouldDetectProviderNotAssigned() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");
        // Device starts with unowned ownership

        // When
        Violations violations = device.toSnapshot().violations();

        // Then
        assertThat(violations.providerNotAssigned()).isTrue();
        assertThat(violations.isValid()).isFalse();
    }

    @Test
    void shouldDetectLocationMissing() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");

        // When
        Violations violations = device.toSnapshot().violations();

        // Then
        assertThat(violations.locationMissing()).isTrue();
        assertThat(violations.isValid()).isFalse();
    }

    @Test
    void shouldDetectShowOnMapButMissingLocation() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");
        device.changeSettings(Settings.of(false, false, false, false, true, true));

        // When
        Violations violations = device.toSnapshot().violations();

        // Then
        assertThat(violations.showOnMapButMissingLocation()).isTrue();
        assertThat(violations.isValid()).isFalse();
    }

    @Test
    void shouldDetectShowOnMapButNoPublicAccess() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");
        device.changeOwnership(Ownership.of("Devicex.nl", "public-devices"));
        device.changeLocation(Location.of(
                "Rakietowa", "1A", "Wrocław", "54-621", null, "POL",
                Coordinates.of(16.931752852309156, 51.09836221719513)
        ));
        device.changeSettings(Settings.of(false, false, false, false, true, false));

        // When
        Violations violations = device.toSnapshot().violations();

        // Then
        assertThat(violations.showOnMapButNoPublicAccess()).isTrue();
        assertThat(violations.isValid()).isFalse();
    }

    @Test
    void shouldHaveNoViolationsWhenFullyConfigured() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");
        device.changeOwnership(Ownership.of("Devicex.nl", "public-devices"));
        device.changeLocation(Location.of(
                "Rakietowa", "1A", "Wrocław", "54-621", null, "POL",
                Coordinates.of(16.931752852309156, 51.09836221719513)
        ));
        device.changeSettings(Settings.of(true, true, true, true, true, true));

        // When
        Violations violations = device.toSnapshot().violations();

        // Then
        assertThat(violations.operatorNotAssigned()).isFalse();
        assertThat(violations.providerNotAssigned()).isFalse();
        assertThat(violations.locationMissing()).isFalse();
        assertThat(violations.showOnMapButMissingLocation()).isFalse();
        assertThat(violations.showOnMapButNoPublicAccess()).isFalse();
        assertThat(violations.isValid()).isTrue();
    }

    @Test
    void shouldCalculateVisibilityAsUsableAndVisibleOnMapWhenFullyConfigured() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");
        device.changeOwnership(Ownership.of("Devicex.nl", "public-devices"));
        device.changeLocation(Location.of(
                "Rakietowa", "1A", "Wrocław", "54-621", null, "POL",
                Coordinates.of(16.931752852309156, 51.09836221719513)
        ));
        device.changeSettings(Settings.of(true, true, true, true, true, true));

        // When
        Visibility visibility = device.toSnapshot().visibility();

        // Then
        assertThat(visibility.forCustomer()).isEqualTo(Visibility.ForCustomer.USABLE_AND_VISIBLE_ON_MAP);
        assertThat(visibility.roamingEnabled()).isTrue();
        assertThat(visibility.isUsable()).isTrue();
        assertThat(visibility.isVisibleOnMap()).isTrue();
    }

    @Test
    void shouldCalculateVisibilityAsInaccessibleWhenViolationsExist() {
        // Given - device with violations
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");
        // Device has unowned ownership, missing location, etc.

        // When
        Visibility visibility = device.toSnapshot().visibility();

        // Then
        assertThat(visibility.forCustomer()).isEqualTo(Visibility.ForCustomer.INACCESSIBLE_AND_HIDDEN_ON_MAP);
        assertThat(visibility.roamingEnabled()).isFalse();
        assertThat(visibility.isInaccessible()).isTrue();
    }

    @Test
    void shouldCalculateVisibilityAsUsableButHiddenWhenNotShowingOnMap() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");
        device.changeOwnership(Ownership.of("Devicex.nl", "public-devices"));
        device.changeLocation(Location.of(
                "Rakietowa", "1A", "Wrocław", "54-621", null, "POL",
                Coordinates.of(16.931752852309156, 51.09836221719513)
        ));
        device.changeSettings(Settings.of(true, true, true, true, false, true)); // showOnMap = false

        // When
        Visibility visibility = device.toSnapshot().visibility();

        // Then
        assertThat(visibility.forCustomer()).isEqualTo(Visibility.ForCustomer.USABLE_BUT_HIDDEN_ON_MAP);
        assertThat(visibility.roamingEnabled()).isTrue();
        assertThat(visibility.isUsable()).isTrue();
        assertThat(visibility.isVisibleOnMap()).isFalse();
    }

    @Test
    void shouldCalculateVisibilityAsInaccessibleWhenNoPublicAccess() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");
        device.changeOwnership(Ownership.of("Devicex.nl", "public-devices"));
        device.changeLocation(Location.of(
                "Rakietowa", "1A", "Wrocław", "54-621", null, "POL",
                Coordinates.of(16.931752852309156, 51.09836221719513)
        ));
        device.changeSettings(Settings.of(true, true, true, true, true, false)); // publicAccess = false

        // When
        Visibility visibility = device.toSnapshot().visibility();

        // Then
        assertThat(visibility.forCustomer()).isEqualTo(Visibility.ForCustomer.INACCESSIBLE_AND_HIDDEN_ON_MAP);
        assertThat(visibility.roamingEnabled()).isFalse();
        assertThat(visibility.isInaccessible()).isTrue();
    }

    @Test
    void shouldHaveNoViolationsWhenNotShowingOnMap() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");
        device.changeOwnership(Ownership.of("Devicex.nl", "public-devices"));
        device.changeLocation(Location.of(
                "Rakietowa", "1A", "Wrocław", "54-621", null, "POL",
                Coordinates.of(16.931752852309156, 51.09836221719513)
        ));
        device.changeSettings(Settings.of(true, true, true, true, false, false));

        // When
        Violations violations = device.toSnapshot().violations();

        // Then
        assertThat(violations.showOnMapButMissingLocation()).isFalse();
        assertThat(violations.showOnMapButNoPublicAccess()).isFalse();
        assertThat(violations.isValid()).isTrue();
    }

    @Test
    void shouldDetectMultipleViolations() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("ALF-98262561");
        device.changeSettings(Settings.of(false, false, false, false, true, false));

        // When
        Violations violations = device.toSnapshot().violations();

        // Then
        assertThat(violations.operatorNotAssigned()).isTrue();
        assertThat(violations.providerNotAssigned()).isTrue();
        assertThat(violations.locationMissing()).isTrue();
        assertThat(violations.showOnMapButMissingLocation()).isTrue();
        assertThat(violations.showOnMapButNoPublicAccess()).isTrue();
        assertThat(violations.isValid()).isFalse();
    }
}
