package devices.configuration.management;

import devices.configuration.IntegrationTest;
import devices.configuration.IntegrationTestConfiguration;
import devices.configuration.tools.AuthFixture;
import devices.configuration.tools.RequestsFixture;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest(profiles = {"auth-test", "integration-test"})
@Import(IntegrationTestConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DeviceConfigurationControllerTest {

  @Autowired AuthFixture auth;
  @Autowired RequestsFixture requests;
  @Autowired DeviceConfigurationService service;

  String deviceId;

  @BeforeEach
  void setUp() {
    requests.withJwt(auth.tokenFor("john", "john"));
    deviceId = "DEV-" + UUID.randomUUID();
    service.createDevice(deviceId);
  }

  @Test
  void shouldGetDeviceConfiguration() {
    requests
        .devices
        .get(deviceId)
        .isExactlyLike(
            """
				{
				    "deviceId": "%s",
				    "ownership": {
				        "operator": null,
				        "provider": null
				    },
				    "location": null,
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
				        "operatorNotAssigned": true,
				        "providerNotAssigned": true,
				        "locationMissing": true,
				        "showOnMapButMissingLocation": false,
				        "showOnMapButNoPublicAccess": false,
				        "valid": false
				    },
				    "visibility": {
				        "roamingEnabled": false,
				        "forCustomer": "INACCESSIBLE_AND_HIDDEN_ON_MAP"
				    }
				}
				""",
            deviceId);
  }

  @Test
  void shouldUpdateOwnership() {
    requests
        .devices
        .patch(
            deviceId,
            """
				{
				    "ownership": {
				        "operator": "Devicex.nl",
				        "provider": "public-devices"
				    }
				}
				""")
        .hasFieldsLike(
            """
				{
				    "deviceId": "%s",
				    "ownership": {
				        "operator": "Devicex.nl",
				        "provider": "public-devices"
				    },
				    "violations": {
				        "operatorNotAssigned": false,
				        "providerNotAssigned": false
				    }
				}
				""",
            deviceId);
    requests
        .devices
        .get(deviceId)
        .hasFieldsLike(
            """
				{
				    "ownership": {
				        "operator": "Devicex.nl",
				        "provider": "public-devices"
				    }
				}
				""");
  }

  @Test
  void shouldSetDeviceUnowned() {
    requests.devices.patch(
        deviceId,
        """
				{
				    "ownership": {
				        "operator": "Devicex.nl",
				        "provider": "public-devices"
				    }
				}
				""");
    requests.devices.patch(
        deviceId,
        """
				{
				    "settings": {
				        "autoStart": true,
				        "remoteControl": true,
				        "billing": true,
				        "reimbursement": true,
				        "showOnMap": true,
				        "publicAccess": true
				    }
				}
				""");
    requests
        .devices
        .patch(
            deviceId,
            """
				{
				    "ownership": {
				        "operator": null,
				        "provider": null
				    }
				}
				""")
        .hasFieldsLike(
            """
				{
				    "deviceId": "%s",
				    "ownership": {
				        "operator": null,
				        "provider": null
				    },
				    "location": null,
				    "settings": {
				        "autoStart": false,
				        "remoteControl": false,
				        "billing": false,
				        "reimbursement": false,
				        "showOnMap": false,
				        "publicAccess": false
				    }
				}
				""",
            deviceId);
  }

  @Test
  void shouldUpdateLocation() {
    requests
        .devices
        .patch(
            deviceId,
            """
				{
				    "location": {
				        "street": "Rakietowa",
				        "houseNumber": "1A",
				        "city": "Wrocław",
				        "postalCode": "54-621",
				        "country": "POL",
				        "coordinates": {
				            "longitude": 16.931752852309156,
				            "latitude": 51.09836221719513
				        }
				    }
				}
				""")
        .hasFieldsLike(
            """
				{
				    "deviceId": "%s",
				    "location": {
				        "street": "Rakietowa",
				        "houseNumber": "1A",
				        "city": "Wrocław",
				        "postalCode": "54-621",
				        "country": "POL",
				        "coordinates": {
				            "longitude": 16.931752852309156,
				            "latitude": 51.09836221719513
				        }
				    },
				    "violations": {
				        "locationMissing": false
				    }
				}
				""",
            deviceId);
  }

  @Test
  void shouldUpdatePartialSettings() {
    requests
        .devices
        .patch(
            deviceId,
            """
				{
				    "settings": {
				        "showOnMap": true,
				        "publicAccess": true
				    }
				}
				""")
        .hasFieldsLike(
            """
				{
				    "deviceId": "%s",
				    "settings": {
				        "autoStart": false,
				        "remoteControl": false,
				        "billing": false,
				        "reimbursement": false,
				        "showOnMap": true,
				        "publicAccess": true
				    }
				}
				""",
            deviceId);
  }

  @Test
  void shouldUpdateMultipleFieldsAtOnce() {
    requests
        .devices
        .patch(
            deviceId,
            """
				{
				    "ownership": {
				        "operator": "Devicex.nl",
				        "provider": "public-devices"
				    },
				    "location": {
				        "street": "Rakietowa",
				        "houseNumber": "2A",
				        "city": "Wrocław",
				        "postalCode": "54-621",
				        "country": "POL",
				        "coordinates": {
				            "longitude": 16.931752852309156,
				            "latitude": 51.09836221719513
				        }
				    },
				    "settings": {
				        "showOnMap": true,
				        "publicAccess": true
				    }
				}
				""")
        .hasFieldsLike(
            """
				{
				    "deviceId": "%s",
				    "ownership": {
				        "operator": "Devicex.nl",
				        "provider": "public-devices"
				    },
				    "location": {
				        "street": "Rakietowa",
				        "houseNumber": "2A"
				    },
				    "settings": {
				        "showOnMap": true,
				        "publicAccess": true
				    },
				    "violations": {
				        "operatorNotAssigned": false,
				        "providerNotAssigned": false,
				        "locationMissing": false,
				        "showOnMapButMissingLocation": false,
				        "showOnMapButNoPublicAccess": false
				    },
				    "visibility": {
				        "roamingEnabled": true,
				        "forCustomer": "USABLE_AND_VISIBLE_ON_MAP"
				    }
				}
				""",
            deviceId);
  }
}
