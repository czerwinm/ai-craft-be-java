package devices.configuration.management;

public class DeviceAlreadyExistsException extends RuntimeException {
  public DeviceAlreadyExistsException(String deviceId) {
    super("Device already exists: " + deviceId);
  }
}
