package devices.configuration.management;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record Visibility(
        ForCustomer forCustomer,
        boolean roamingEnabled
) {

    public enum ForCustomer {
        USABLE_AND_VISIBLE_ON_MAP,
        USABLE_BUT_HIDDEN_ON_MAP,
        INACCESSIBLE_AND_HIDDEN_ON_MAP
    }

    /**
     * Factory method to calculate visibility based on violations, settings, and showOnMap flag.
     * 
     * @param violations current device configuration violations
     * @param publicAccess whether the device has public access enabled
     * @param showOnMap whether the device should be shown on the map
     * @return calculated Visibility object
     */
    public static Visibility calculateFrom(Violations violations, boolean publicAccess, boolean showOnMap) {
        boolean isUsable = violations.isValid() && publicAccess;
        boolean roamingEnabled = isUsable;
        
        ForCustomer forCustomer;
        if (!isUsable) {
            forCustomer = ForCustomer.INACCESSIBLE_AND_HIDDEN_ON_MAP;
        } else if (showOnMap) {
            forCustomer = ForCustomer.USABLE_AND_VISIBLE_ON_MAP;
        } else {
            forCustomer = ForCustomer.USABLE_BUT_HIDDEN_ON_MAP;
        }
        
        return new Visibility(forCustomer, roamingEnabled);
    }

    @JsonIgnore
    public boolean isUsable() {
        return forCustomer == ForCustomer.USABLE_AND_VISIBLE_ON_MAP 
                || forCustomer == ForCustomer.USABLE_BUT_HIDDEN_ON_MAP;
    }

    @JsonIgnore
    public boolean isVisibleOnMap() {
        return forCustomer == ForCustomer.USABLE_AND_VISIBLE_ON_MAP;
    }

    @JsonIgnore
    public boolean isInaccessible() {
        return forCustomer == ForCustomer.INACCESSIBLE_AND_HIDDEN_ON_MAP;
    }
}
