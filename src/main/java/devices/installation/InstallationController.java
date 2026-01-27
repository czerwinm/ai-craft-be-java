package devices.installation;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import devices.configuration.management.Coordinates;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/installations")
class InstallationController {

  private final InstallationService service;

  @GetMapping(path = "/{workOrderId}", produces = APPLICATION_JSON_VALUE)
  InstallationSnapshot get(@PathVariable String workOrderId) {
    return service
        .getInstallation(WorkOrderId.of(workOrderId))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @PostMapping(
      path = "/{workOrderId}/assign",
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  InstallationSnapshot assignInstaller(
      @PathVariable String workOrderId, @RequestBody AssignInstallerRequest request) {
    return service
        .assignInstaller(
            WorkOrderId.of(workOrderId),
            InstallerId.of(request.installerId()),
            request.forceReassign())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @PostMapping(path = "/{workOrderId}/start", produces = APPLICATION_JSON_VALUE)
  InstallationSnapshot start(@PathVariable String workOrderId) {
    return service
        .startInstallation(WorkOrderId.of(workOrderId))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @PostMapping(
      path = "/{workOrderId}/device",
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  InstallationSnapshot assignDevice(
      @PathVariable String workOrderId, @RequestBody AssignDeviceRequest request) {
    return service
        .assignDevice(WorkOrderId.of(workOrderId), DeviceIdentifier.of(request.deviceIdentifier()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @PostMapping(path = "/{workOrderId}/boot-confirmation", produces = APPLICATION_JSON_VALUE)
  InstallationSnapshot confirmBoot(@PathVariable String workOrderId) {
    return service
        .confirmBoot(WorkOrderId.of(workOrderId))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @PostMapping(
      path = "/{workOrderId}/location",
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  InstallationSnapshot setLocation(
      @PathVariable String workOrderId, @RequestBody SetLocationRequest request) {
    return service
        .setLocation(WorkOrderId.of(workOrderId), request.coordinates())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @PostMapping(path = "/{workOrderId}/finish", produces = APPLICATION_JSON_VALUE)
  InstallationSnapshot finish(@PathVariable String workOrderId) {
    return service
        .finishInstallation(WorkOrderId.of(workOrderId))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  record AssignInstallerRequest(String installerId, boolean forceReassign) {}

  record AssignDeviceRequest(String deviceIdentifier) {}

  record SetLocationRequest(Coordinates coordinates) {}
}
