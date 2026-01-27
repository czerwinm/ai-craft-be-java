package devices.installation;

import devices.configuration.management.Coordinates;
import devices.configuration.management.Ownership;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class InstallationService {

  private final InstallationRepository repository;

  @Transactional(readOnly = true)
  public Optional<InstallationSnapshot> getInstallation(WorkOrderId workOrderId) {
    return repository.get(workOrderId).map(Installation::toSnapshot);
  }

  public InstallationSnapshot createFromWorkOrder(
      WorkOrderId workOrderId, InstallationContext context, Ownership ownership) {
    Installation installation = Installation.fromWorkOrder(workOrderId, context, ownership);
    repository.save(installation);
    return installation.toSnapshot();
  }

  public Optional<InstallationSnapshot> assignInstaller(
      WorkOrderId workOrderId, InstallerId installerId, boolean force) {
    return repository
        .get(workOrderId)
        .map(
            installation -> {
              installation.assignInstaller(installerId, force);
              repository.save(installation);
              return installation.toSnapshot();
            });
  }

  public Optional<InstallationSnapshot> startInstallation(WorkOrderId workOrderId) {
    return repository
        .get(workOrderId)
        .map(
            installation -> {
              installation.start();
              repository.save(installation);
              return installation.toSnapshot();
            });
  }

  public Optional<InstallationSnapshot> assignDevice(
      WorkOrderId workOrderId, DeviceIdentifier deviceIdentifier) {
    return repository
        .get(workOrderId)
        .map(
            installation -> {
              installation.assignDevice(deviceIdentifier);
              repository.save(installation);
              return installation.toSnapshot();
            });
  }

  public Optional<InstallationSnapshot> registerBootNotification(
      WorkOrderId workOrderId, BootDetails bootDetails) {
    return repository
        .get(workOrderId)
        .map(
            installation -> {
              installation.registerBootNotification(bootDetails);
              repository.save(installation);
              return installation.toSnapshot();
            });
  }

  public Optional<InstallationSnapshot> confirmBoot(WorkOrderId workOrderId) {
    return repository
        .get(workOrderId)
        .map(
            installation -> {
              installation.confirmBoot();
              repository.save(installation);
              return installation.toSnapshot();
            });
  }

  public Optional<InstallationSnapshot> setLocation(
      WorkOrderId workOrderId, Coordinates coordinates) {
    return repository
        .get(workOrderId)
        .map(
            installation -> {
              installation.setLocation(coordinates);
              repository.save(installation);
              return installation.toSnapshot();
            });
  }

  public Optional<InstallationSnapshot> finishInstallation(WorkOrderId workOrderId) {
    return repository
        .get(workOrderId)
        .map(
            installation -> {
              installation.finish();
              repository.save(installation);
              return installation.toSnapshot();
            });
  }

  public Optional<InstallationSnapshot> markSatAnswersComplete(WorkOrderId workOrderId) {
    return repository
        .get(workOrderId)
        .map(
            installation -> {
              installation.markSatAnswersComplete();
              repository.save(installation);
              return installation.toSnapshot();
            });
  }
}
