package devices.installation;

import static org.assertj.core.api.Assertions.assertThat;

import devices.configuration.management.Coordinates;
import devices.configuration.management.Ownership;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InstallationServiceTest {

  private InstallationService service;
  private InstallationRepository repository;

  @BeforeEach
  void setUp() {
    repository = new InMemoryInstallationRepository();
    service = new InstallationService(repository);
  }

  @Test
  void shouldCreateInstallationFromWorkOrder() {
    // Given
    WorkOrderId workOrderId = WorkOrderId.of("WO-12345");
    InstallationContext context = InstallationContext.of("corporate", "POL", "wallbox", null);
    Ownership ownership = Ownership.of("Devicex.nl", "public-devices");

    // When
    InstallationSnapshot result = service.createFromWorkOrder(workOrderId, context, ownership);

    // Then
    assertThat(result.workOrderId()).isEqualTo(workOrderId);
    assertThat(result.phase()).isEqualTo(InstallationPhase.INITIAL);
    assertThat(result.ownership()).isEqualTo(ownership);
  }

  @Test
  void shouldAssignInstallerToWorkOrder() {
    // Given
    WorkOrderId workOrderId = WorkOrderId.of("WO-12345");
    service.createFromWorkOrder(
        workOrderId,
        InstallationContext.of("corporate", "POL", "wallbox", null),
        Ownership.of("Devicex.nl", "public-devices"));

    // When
    InstallationSnapshot result =
        service.assignInstaller(workOrderId, InstallerId.of("installer-123"), false).orElseThrow();

    // Then
    assertThat(result.assignedInstallerId()).isEqualTo(InstallerId.of("installer-123"));
  }

  @Test
  void shouldCompleteFullInstallationFlow() {
    // Given
    WorkOrderId workOrderId = WorkOrderId.of("WO-12345");
    service.createFromWorkOrder(
        workOrderId,
        InstallationContext.of("corporate", "POL", "wallbox", null),
        Ownership.of("Devicex.nl", "public-devices"));

    // When - full flow
    service.assignInstaller(workOrderId, InstallerId.of("installer-1"), false);
    service.startInstallation(workOrderId);
    service.assignDevice(workOrderId, DeviceIdentifier.of("DEV-123"));

    BootDetails boot = BootDetails.of("Garo", "CPF25", "1.0", "S123", "IoT16");
    service.registerBootNotification(workOrderId, boot);
    service.confirmBoot(workOrderId);

    service.setLocation(workOrderId, Coordinates.of(16.931752, 51.098362));
    service.markSatAnswersComplete(workOrderId);

    InstallationSnapshot result = service.finishInstallation(workOrderId).orElseThrow();

    // Then
    assertThat(result.phase()).isEqualTo(InstallationPhase.FINISHED);
    assertThat(result.canFinish()).isTrue();
  }
}
