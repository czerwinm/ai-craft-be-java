package devices.installation;

public record InstallerId(String value) {

  public InstallerId {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("InstallerId cannot be null or blank");
    }
  }

  public static InstallerId of(String value) {
    return new InstallerId(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
