package devices.configuration.management;

import lombok.Builder;

@Builder
public record Violations(
        boolean operatorNotAssigned,
        boolean providerNotAssigned,
        boolean locationMissing,
        boolean showOnMapButMissingLocation,
        boolean showOnMapButNoPublicAccess
) {

    public boolean isValid() {
        return !operatorNotAssigned
                && !providerNotAssigned
                && !locationMissing
                && !showOnMapButMissingLocation
                && !showOnMapButNoPublicAccess;
    }
}
