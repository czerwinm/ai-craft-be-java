package devices.configuration.management;

public record Location(
        String street,
        String houseNumber,
        String city,
        String postalCode,
        String state,
        String country,
        Coordinates coordinates
) {

    public Location {
        if (coordinates == null) {
            throw new IllegalArgumentException("Coordinates are mandatory");
        }
    }

    public static Location of(
            String street,
            String houseNumber,
            String city,
            String postalCode,
            String state,
            String country,
            Coordinates coordinates
    ) {
        return new Location(street, houseNumber, city, postalCode, state, country, coordinates);
    }
}
