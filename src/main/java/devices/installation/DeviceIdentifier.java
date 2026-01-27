package devices.installation;

public record DeviceIdentifier(String value) {

  public DeviceIdentifier {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("DeviceIdentifier cannot be null or blank");
    }
  }

  public static DeviceIdentifier of(String value) {
    return new DeviceIdentifier(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
