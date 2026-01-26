package devices.configuration.management;

import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@AllArgsConstructor
class DeviceConfiguration {

    final String deviceId;
    final List<DomainEvent> events;
    private Ownership ownership;
    private Location location;
    private OpeningHours openingHours;
    private Settings settings;

    static DeviceConfiguration newDevice(String deviceId) {
        DeviceConfiguration device = new DeviceConfiguration(
                deviceId,
                new ArrayList<>(),
                Ownership.unowned(),
                null,
                OpeningHours.alwaysOpened(),
                Settings.defaultSettings()
        );
        device.events.add(new DomainEvent.DeviceCreated(deviceId));
        return device;
    }

    void changeOwnership(Ownership newOwnership) {
        Objects.requireNonNull(newOwnership, "Ownership cannot be null");

        if (!Objects.equals(this.ownership, newOwnership)) {
            this.ownership = newOwnership;
            events.add(new DomainEvent.OwnershipChanged(deviceId, newOwnership));

            if (newOwnership.isUnowned()) {
                resetToDefaults();
            }
        }
    }

    void changeLocation(Location newLocation) {
        if (!Objects.equals(this.location, newLocation)) {
            this.location = newLocation;
            events.add(new DomainEvent.LocationChanged(deviceId, newLocation));
            events.add(new DomainEvent.DeviceConfigurationChanged(deviceId));
        }
    }

    void changeOpeningHours(OpeningHours newOpeningHours) {
        Objects.requireNonNull(newOpeningHours, "OpeningHours cannot be null");

        if (!Objects.equals(this.openingHours, newOpeningHours)) {
            this.openingHours = newOpeningHours;
            events.add(new DomainEvent.OpeningHoursChanged(deviceId, newOpeningHours));
            events.add(new DomainEvent.DeviceConfigurationChanged(deviceId));
        }
    }

    void changeSettings(Settings newSettings) {
        Objects.requireNonNull(newSettings, "Settings cannot be null");

        if (!Objects.equals(this.settings, newSettings)) {
            this.settings = newSettings;
            events.add(new DomainEvent.SettingsChanged(deviceId, newSettings));
            events.add(new DomainEvent.DeviceConfigurationChanged(deviceId));
        }
    }

    private void resetToDefaults() {
        changeLocation(null);
        changeOpeningHours(OpeningHours.alwaysOpened());
        changeSettings(Settings.defaultSettings());
    }

    private Violations checkViolations() {
        return Violations.builder()
                .operatorNotAssigned(ownership.isUnowned())
                .providerNotAssigned(ownership.isUnowned())
                .locationMissing(location == null)
                .showOnMapButMissingLocation(settings.showOnMap() && location == null)
                .showOnMapButNoPublicAccess(settings.showOnMap() && !settings.publicAccess())
                .build();
    }

    DeviceConfigurationSnapshot toSnapshot() {
        return new DeviceConfigurationSnapshot(
                deviceId,
                ownership,
                location,
                openingHours,
                settings,
                checkViolations()
        );
    }
}
