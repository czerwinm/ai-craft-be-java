package devices.installation;

public enum InstallationPhase {
  INITIAL,
  DEVICE_ASSIGNED,
  BOOT_CONFIRMATION_REQUIRED,
  BOOT_CONFIRMED,
  LOCATION_SET,
  FINAL,
  FINISHED
}
