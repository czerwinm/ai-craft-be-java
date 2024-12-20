package devices.configuration.installations;

import devices.configuration.device.Location;
import devices.configuration.device.Ownership;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Primary
@Component
class DevicesClient implements Devices {

    private final RestTemplate rest;

    public DevicesClient(RestTemplateBuilder builder) {
        this.rest = builder
                .rootUri("http://devices-service.cpo-namespace")
                .build();
    }

    @Override
    public void create(String deviceId, Ownership ownership, Location location) {
        rest.put("/devices/{deviceId}",
                new CreateDevice(location, ownership), deviceId
        );
    }

    record CreateDevice(Location location, Ownership ownership) {}

}
