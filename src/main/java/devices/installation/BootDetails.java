package devices.installation;

public record BootDetails(
    String vendor, String model, String firmware, String serial, String protocol) {

  public BootDetails {
    if (vendor == null || vendor.isBlank()) {
      throw new IllegalArgumentException("Vendor cannot be null or blank");
    }
    if (model == null || model.isBlank()) {
      throw new IllegalArgumentException("Model cannot be null or blank");
    }
    if (serial == null || serial.isBlank()) {
      throw new IllegalArgumentException("Serial cannot be null or blank");
    }
    if (protocol == null || protocol.isBlank()) {
      throw new IllegalArgumentException("Protocol cannot be null or blank");
    }
  }

  public static BootDetails of(
      String vendor, String model, String firmware, String serial, String protocol) {
    return new BootDetails(vendor, model, firmware, serial, protocol);
  }

  public boolean isDifferentHardwareFrom(BootDetails other) {
    if (other == null) {
      return true;
    }
    return !vendor.equals(other.vendor)
        || !model.equals(other.model)
        || !serial.equals(other.serial)
        || !protocol.equals(other.protocol);
  }

  public boolean hasDifferentFirmwareFrom(BootDetails other) {
    if (other == null) {
      return true;
    }
    return firmware != null && !firmware.equals(other.firmware);
  }
}
