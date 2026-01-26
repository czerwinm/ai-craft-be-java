package devices.configuration.management;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
class DeviceConfigurationController {

  private final DeviceConfigurationService service;

  @GetMapping(path = "/devices/{deviceId}", produces = APPLICATION_JSON_VALUE)
  DeviceConfigurationSnapshot getDevice(@PathVariable String deviceId) {
    return service
        .getDevice(deviceId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @PatchMapping(
      path = "/devices/{deviceId}",
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  DeviceConfigurationSnapshot patchDevice(
      @PathVariable String deviceId,
      @RequestHeader(value = "If-Match", required = false) Long version,
      @RequestBody DeviceConfigurationPatch patch) {
    try {
      long effectiveVersion = version != null ? version : getCurrentVersion(deviceId);
      return service.patchDevice(deviceId, patch, effectiveVersion);
    } catch (DeviceNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (OptimisticLockException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
  }

  private long getCurrentVersion(String deviceId) {
    return service.getCurrentVersion(deviceId);
  }
}
