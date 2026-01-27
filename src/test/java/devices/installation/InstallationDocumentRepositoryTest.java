package devices.installation;

import static org.assertj.core.api.Assertions.assertThat;

import devices.configuration.AppRunner;
import devices.configuration.IntegrationTestConfiguration;
import devices.configuration.management.Coordinates;
import devices.configuration.management.Ownership;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = AppRunner.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@Transactional
@Testcontainers
@RecordApplicationEvents
@Import(IntegrationTestConfiguration.class)
class InstallationDocumentRepositoryTest {

  @Autowired InstallationRepository repository;

  @Test
  void shouldSaveAndRetrieveInstallation() {
    // Given
    WorkOrderId workOrderId = WorkOrderId.of("WO-TEST-001");
    InstallationContext context = InstallationContext.of("corporate", "POL", "wallbox", null);
    Ownership ownership = Ownership.of("Devicex.nl", "public-devices");
    Installation installation = Installation.fromWorkOrder(workOrderId, context, ownership);

    // When
    repository.save(installation);
    Optional<Installation> retrieved = repository.get(workOrderId);

    // Then
    assertThat(retrieved).isPresent();
    InstallationSnapshot snapshot = retrieved.get().toSnapshot();
    assertThat(snapshot.workOrderId()).isEqualTo(workOrderId);
    assertThat(snapshot.phase()).isEqualTo(InstallationPhase.INITIAL);
    assertThat(snapshot.ownership()).isEqualTo(ownership);
  }

  @Test
  void shouldReturnEmptyForNonExistentInstallation() {
    // When
    Optional<Installation> result = repository.get(WorkOrderId.of("NON-EXISTENT"));

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void shouldUpdateExistingInstallation() {
    // Given
    WorkOrderId workOrderId = WorkOrderId.of("WO-TEST-002");
    InstallationContext context = InstallationContext.of("corporate", "POL", "wallbox", null);
    Ownership ownership = Ownership.of("Devicex.nl", "public-devices");
    Installation installation = Installation.fromWorkOrder(workOrderId, context, ownership);
    repository.save(installation);

    // When - update
    Installation retrieved = repository.get(workOrderId).orElseThrow();
    retrieved.assignInstaller(InstallerId.of("installer-1"), false);
    retrieved.assignDevice(DeviceIdentifier.of("DEV-123"));
    repository.save(retrieved);

    // Then
    Installation updated = repository.get(workOrderId).orElseThrow();
    InstallationSnapshot snapshot = updated.toSnapshot();
    assertThat(snapshot.assignedInstallerId()).isEqualTo(InstallerId.of("installer-1"));
    assertThat(snapshot.deviceIdentifier()).isEqualTo(DeviceIdentifier.of("DEV-123"));
    assertThat(snapshot.phase()).isEqualTo(InstallationPhase.DEVICE_ASSIGNED);
  }

  @Test
  void shouldHandleCompleteInstallationFlow() {
    // Given
    WorkOrderId workOrderId = WorkOrderId.of("WO-TEST-003");
    Installation installation =
        Installation.fromWorkOrder(
            workOrderId,
            InstallationContext.of("corporate", "POL", "wallbox", null),
            Ownership.of("Devicex.nl", "public-devices"));
    repository.save(installation);

    // When - complete flow
    Installation retrieved = repository.get(workOrderId).orElseThrow();
    retrieved.assignInstaller(InstallerId.of("installer-1"), false);
    retrieved.start();
    retrieved.assignDevice(DeviceIdentifier.of("DEV-123"));
    BootDetails boot = BootDetails.of("Garo", "CPF25", "1.0", "S123", "IoT16");
    retrieved.registerBootNotification(boot);
    retrieved.confirmBoot();
    retrieved.setLocation(Coordinates.of(16.931752, 51.098362));
    retrieved.markSatAnswersComplete();
    retrieved.finish();
    repository.save(retrieved);

    // Then
    Installation finished = repository.get(workOrderId).orElseThrow();
    InstallationSnapshot snapshot = finished.toSnapshot();
    assertThat(snapshot.phase()).isEqualTo(InstallationPhase.FINISHED);
    assertThat(snapshot.canFinish()).isTrue();
  }
}
