package devices.configuration.management;

public record VersionedDevice(DeviceConfiguration device, long version) {
  public VersionedDevice {
    if (device == null) {
      throw new IllegalArgumentException("Device cannot be null");
    }
    if (version < 0) {
      throw new IllegalArgumentException("Version must be non-negative");
    }
  }
}
