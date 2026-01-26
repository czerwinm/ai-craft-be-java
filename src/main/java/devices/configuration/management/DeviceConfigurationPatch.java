package devices.configuration.management;

public record DeviceConfigurationPatch(
        Ownership ownership,
        Location location,
        OpeningHours openingHours,
        PartialSettings settings
) {

    void applyTo(DeviceConfiguration device, DeviceConfigurationSnapshot currentSnapshot) {
        if (ownership != null) {
            device.changeOwnership(ownership);
        }
        if (location != null) {
            device.changeLocation(location);
        }
        if (openingHours != null) {
            device.changeOpeningHours(openingHours);
        }
        if (settings != null) {
            Settings mergedSettings = settings.mergeWith(currentSnapshot.settings());
            device.changeSettings(mergedSettings);
        }
    }

    public record PartialSettings(
            Boolean autoStart,
            Boolean remoteControl,
            Boolean billing,
            Boolean reimbursement,
            Boolean showOnMap,
            Boolean publicAccess
    ) {
        Settings mergeWith(Settings current) {
            return Settings.of(
                    autoStart != null ? autoStart : current.autoStart(),
                    remoteControl != null ? remoteControl : current.remoteControl(),
                    billing != null ? billing : current.billing(),
                    reimbursement != null ? reimbursement : current.reimbursement(),
                    showOnMap != null ? showOnMap : current.showOnMap(),
                    publicAccess != null ? publicAccess : current.publicAccess()
            );
        }
    }
}
