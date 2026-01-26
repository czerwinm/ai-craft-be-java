package devices.configuration.management;

public record OpeningHours(boolean alwaysOpen) {

    public static OpeningHours alwaysOpened() {
        return new OpeningHours(true);
    }

    public static OpeningHours of(boolean alwaysOpen) {
        return new OpeningHours(alwaysOpen);
    }
}
