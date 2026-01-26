package devices.configuration.communication;

import lombok.AllArgsConstructor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
class IntervalConfiguration {

    private final List<IntervalRule.DeviceIdRule> deviceIdRules;
    private final List<IntervalRule.ModelRule> modelRules;
    private final IntervalRule.DefaultRule defaultRule;

    static IntervalConfiguration create(
            List<IntervalRule.DeviceIdRule> deviceIdRules,
            List<IntervalRule.ModelRule> modelRules,
            IntervalRule.DefaultRule defaultRule) {
        return new IntervalConfiguration(
                new ArrayList<>(deviceIdRules),
                new ArrayList<>(modelRules),
                defaultRule
        );
    }

    Duration calculateInterval(BootNotification boot) {
        for (IntervalRule.DeviceIdRule rule : deviceIdRules) {
            if (rule.matches(boot)) {
                return rule.interval();
            }
        }
        for (IntervalRule.ModelRule rule : modelRules) {
            if (rule.matches(boot)) {
                return rule.interval();
            }
        }
        return defaultRule.interval();
    }
}
