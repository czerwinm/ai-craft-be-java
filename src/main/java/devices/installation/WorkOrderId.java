package devices.installation;

public record WorkOrderId(String value) {

  public WorkOrderId {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("WorkOrderId cannot be null or blank");
    }
  }

  public static WorkOrderId of(String value) {
    return new WorkOrderId(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
