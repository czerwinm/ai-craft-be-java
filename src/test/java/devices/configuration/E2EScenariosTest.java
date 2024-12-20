package devices.configuration;

import devices.configuration.device.DeviceFixture;
import devices.configuration.installations.InstallationService;
import devices.configuration.tools.AuthFixture;
import devices.configuration.tools.KafkaFixture;
import devices.configuration.tools.RequestsFixture;
import devices.configuration.tools.RestTemplateFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@IntegrationTest(profiles = {"auth-test", "kafka-test", "integration-test"})
class E2EScenariosTest {

    @Autowired
    AuthFixture auth;
    @Autowired
    RequestsFixture requests;
    @Autowired
    InstallationService service;
    @Autowired
    KafkaFixture kafka;
    @Autowired
    RestTemplateFixture rest;
    private MockRestServiceServer installations2DeviceClient;

    final String orderId = DeviceFixture.randomId();
    final String deviceId = DeviceFixture.randomId();

    @BeforeEach
    void setUp() {
        installations2DeviceClient = rest.getRestTemplate("devicesClient", "rest")
                .overrideToLocalServer()
                .interceptToMockRestServiceServer();
        requests.withJwt(auth.tokenFor("john", "john"));
    }

    @Test
    void fullInstallationAndConfigurationOfDevice() {
        // when
        kafka.publish("sales.work-orders", orderId, """
                {
                  "id": "%s",
                  "tenant": "Devicex.nl",
                  "account": "public-devices"
                }
                """, orderId);

        // given
        requests.installations.get(0, 10000).isExactlyLike("""
                {"content":[{"orderId":"%s","deviceId":null,"state":"PENDING"}],"totalPages":1,"totalElements":1,"page":0,"size":1}""", orderId);
        requests.installations.get(orderId).isExactlyLike("""
                {"orderId":"%s","deviceId":null,"state":"PENDING"} """, orderId);

        // when
        requests.installations.patch(orderId, """
                        { "assignDevice": "%s" } """, deviceId)
                .isExactlyLike("""
                        {"orderId":"%s","deviceId":"%s","state":"DEVICE_ASSIGNED"}""", orderId, deviceId);

        // when
        requests.installations.patch(orderId, """
                        {
                          "assignLocation": {
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
                        }""")
                .isExactlyLike("""
                        {"orderId":"%s","deviceId":"%s","state":"DEVICE_ASSIGNED"}
                        """, orderId, deviceId);

        requests.communication.bootIot16(deviceId, """
                        {
                          "chargePointVendor": "Garo",
                          "chargePointModel": "CPF25 Family",
                          "chargePointSerialNumber": "820394A93203",
                          "chargeBoxSerialNumber": "891234A56711",
                          "firmwareVersion": "1.1",
                          "iccid": "112233445566778899C1",
                          "imsi": "082931213347973812",
                          "meterType": "5051",
                          "meterSerialNumber": "937462A48276"
                        }
                        """)
                .hasFieldsLike("""
                        {"interval":1800,"status":"Pending"}
                        """, orderId, deviceId);

        requests.installations.get(orderId)
                .isExactlyLike("""
                        {"orderId":"%s","deviceId":"%s","state":"BOOTED"}
                        """, orderId, deviceId);

        requests.installations.patch(orderId, """
                        { "confirmBoot": true }""")
                .isExactlyLike("""
                        {"orderId":"%s","deviceId":"%s","state":"BOOTED"}""", orderId, deviceId);

        requests.intervals.put("""
                {
                  "byIds": [ { "seconds": 600, "devices": [ "%s" ] } ],
                  "byModel": [ ],
                  "defSeconds": 1800
                }
                """, deviceId);

        installations2DeviceClient.expect(requestTo(STR."http://devices-service.cpo-namespace/devices/\{deviceId}"))
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

        requests.installations.patch(orderId, """
                        { "complete": true }""")
                .isExactlyLike("""
                        {"orderId":"%s","deviceId":"%s","state":"COMPLETED"}""", orderId, deviceId);

        requests.communication.bootIot16(deviceId, """
                        {
                          "chargePointVendor": "Garo",
                          "chargePointModel": "CPF25 Family",
                          "chargePointSerialNumber": "820394A93203",
                          "chargeBoxSerialNumber": "891234A56711",
                          "firmwareVersion": "1.13",
                          "iccid": "112233445566778899C1",
                          "imsi": "082931213347973812",
                          "meterType": "5051",
                          "meterSerialNumber": "937462A48276"
                        }
                        """)
                .hasFieldsLike("""
                        {"interval":600,"status":"Accepted"}
                        """, orderId, deviceId);

        requests.devices.get(deviceId).isExactlyLike("""
                {
                  "deviceId": "%s",
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
                  },
                  "openingHours": {
                    "alwaysOpen": true
                  },
                  "settings": {
                    "autoStart": false,
                    "remoteControl": false,
                    "billing": false,
                    "reimbursement": false,
                    "showOnMap": false,
                    "publicAccess": false
                  },
                  "violations": {
                    "operatorNotAssigned": false,
                    "providerNotAssigned": false,
                    "locationMissing": false,
                    "showOnMapButMissingLocation": false,
                    "showOnMapButNoPublicAccess": false
                  },
                  "visibility": {
                    "roamingEnabled": false,
                    "forCustomer": "INACCESSIBLE_AND_HIDDEN_ON_MAP"
                  },
                  "boot": {
                    "protocol": "IoT16",
                    "vendor": "Garo",
                    "model": "CPF25 Family",
                    "serial": "891234A56711",
                    "firmware": "1.13"
                  }
                }
                """, deviceId);

        requests.devices.patch(deviceId, """
                {
                  "settings": {
                    "publicAccess": true,
                    "showOnMap": true
                  }
                }
                """).hasFieldsLike("""
                {
                  "deviceId": "%s",
                  "settings": {
                    "showOnMap": true,
                    "publicAccess": true
                  },
                  "visibility": {
                    "forCustomer": "USABLE_AND_VISIBLE_ON_MAP"
                  }
                }
                """, deviceId);

        requests.devices.get(deviceId).hasFieldsLike("""
                {
                  "deviceId": "%s",
                  "settings": {
                    "showOnMap": true,
                    "publicAccess": true
                  },
                  "visibility": {
                    "forCustomer": "USABLE_AND_VISIBLE_ON_MAP"
                  }
                }
                """, deviceId);
    }
}
