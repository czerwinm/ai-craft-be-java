package devices.configuration.communication;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@AllArgsConstructor
public class HeartbeatIntervalCalculator implements HeartbeatInterval {

    private final IntervalConfiguration configuration;

    @Override
    public Duration heartbeatIntervalFor(BootNotification boot) {
        return configuration.calculateInterval(boot);
    }
}
