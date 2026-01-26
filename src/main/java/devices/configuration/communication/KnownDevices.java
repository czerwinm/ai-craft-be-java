package devices.configuration.communication;

public interface KnownDevices {
  enum State {
    UNKNOWN,
    IN_INSTALLATION,
    EXISTING
  }

  State queryDevice(String deviceId);
}
