package devices.configuration.communication;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "intervals")
@Component
@Getter
@Setter
class IntervalConfigurationProperties {

  private List<DeviceIdRuleProperties> deviceIdRules;
  private List<ModelRuleProperties> modelRules;
  private DefaultRuleProperties defaultRule;

  record DeviceIdRuleProperties(Duration interval, Set<String> deviceIds) {}

  record ModelRuleProperties(Duration interval, String vendor, String modelRegex) {}

  record DefaultRuleProperties(Duration interval) {}

  IntervalConfiguration toIntervalConfiguration() {
    List<IntervalRule.DeviceIdRule> deviceRules =
        deviceIdRules.stream()
            .map(p -> new IntervalRule.DeviceIdRule(p.interval(), p.deviceIds()))
            .toList();
    List<IntervalRule.ModelRule> modelRulesList =
        modelRules.stream()
            .map(p -> new IntervalRule.ModelRule(p.interval(), p.vendor(), p.modelRegex()))
            .toList();
    IntervalRule.DefaultRule defaultRuleObj =
        new IntervalRule.DefaultRule(defaultRule.interval());
    return IntervalConfiguration.create(deviceRules, modelRulesList, defaultRuleObj);
  }
}
