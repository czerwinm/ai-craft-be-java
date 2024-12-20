package devices.configuration.device;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequiredArgsConstructor
class DeviceController {

    private final DeviceService service;

    @PutMapping(path = "/devices/{deviceId}",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE)
    DeviceConfiguration put(@PathVariable String deviceId,
                            @RequestBody @Valid UpdateDevice update) {
        return service.createNewDevice(deviceId, update);
    }

    @PatchMapping(path = "/devices/{deviceId}",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE)
    DeviceConfiguration patch(@PathVariable String deviceId,
                              @RequestBody @Valid UpdateDevice update) {
        return service.updateDevice(deviceId, update)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
