package devices.configuration.management;

import lombok.Builder;

@Builder(toBuilder = true)
public record Settings(
        boolean autoStart,
        boolean remoteControl,
        boolean billing,
        boolean reimbursement,
        boolean showOnMap,
        boolean publicAccess
) {

    public static Settings defaultSettings() {
        return new Settings(false, false, false, false, false, false);
    }

    public static Settings of(
            boolean autoStart,
            boolean remoteControl,
            boolean billing,
            boolean reimbursement,
            boolean showOnMap,
            boolean publicAccess
    ) {
        return new Settings(autoStart, remoteControl, billing, reimbursement, showOnMap, publicAccess);
    }
}
