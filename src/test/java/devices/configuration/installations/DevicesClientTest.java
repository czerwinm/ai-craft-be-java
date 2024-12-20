package devices.configuration.installations;

import devices.configuration.device.DeviceFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(DevicesClient.class)
class DevicesClientTest {

    @Autowired
    private DevicesClient client;
    @Autowired
    private MockRestServiceServer server;

    @Test
    void createNewDevice() {
        server.expect(requestTo("/devices/deviceId"))
                .andExpect(method(PUT))
                .andExpect(content().json("""
                        {
                          "ownership": {
                            "operator": "Devicex.nl",
                            "provider": "public-devices"
                          },
                          "location": {
                            "street": "Rakietowa",
                            "houseNumber": "1A",
                            "city": "Wrocław",
                            "postalCode": "54-621",
                            "state": null,
                            "country": "POL",
                            "coordinates": {
                              "longitude": 51.09836221719513,
                              "latitude": 16.931752852309156
                            }
                          }
                        }
                        """))
                .andRespond(withSuccess());

        client.create("deviceId", DeviceFixture.ownership(), DeviceFixture.location());
    }
}
