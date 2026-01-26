package devices.configuration.communication.protocols.iot16;

import devices.configuration.tools.JsonConfiguration;

public class Ocpp16MessagesFixture {

  public static BootNotificationRequest bootNotification() {
    return new BootNotificationRequest(
        "Garo",
        "CPF25 Family",
        "820394A93203",
        "891234A56711",
        "1.1",
        "112233445566778899C1",
        "082931213347973812",
        "5051",
        "937462A48276");
  }

  public static String json() {
    return JsonConfiguration.json(bootNotification());
  }
}
