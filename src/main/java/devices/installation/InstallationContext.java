package devices.installation;

public record InstallationContext(
    String segment, String country, String equipmentCategory, String customerId) {

  public InstallationContext {
    if (segment == null || segment.isBlank()) {
      throw new IllegalArgumentException("Segment cannot be null or blank");
    }
    if (country == null || country.isBlank()) {
      throw new IllegalArgumentException("Country cannot be null or blank");
    }
  }

  public static InstallationContext of(
      String segment, String country, String equipmentCategory, String customerId) {
    return new InstallationContext(segment, country, equipmentCategory, customerId);
  }
}
