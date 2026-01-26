package devices.configuration.management;

public record DeviceConfigurationSnapshot(
        String deviceId,
        Ownership ownership,
        Location location,
        OpeningHours openingHours,
        Settings settings,
        Violations violations
) {
}
