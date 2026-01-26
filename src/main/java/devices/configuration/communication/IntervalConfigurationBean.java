package devices.configuration.communication;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Configuration
class IntervalConfigurationBean {

    @Bean
    IntervalConfiguration intervalConfiguration() {
        return IntervalConfiguration.create(
                createDeviceIdRules(),
                createModelRules(),
                createDefaultRule()
        );
    }

    private List<IntervalRule.DeviceIdRule> createDeviceIdRules() {
        return List.of(
                new IntervalRule.DeviceIdRule(
                        Duration.ofSeconds(600),
                        Set.of("EVB-P4562137", "ALF-9571445")
                ),
                new IntervalRule.DeviceIdRule(
                        Duration.ofSeconds(2700),
                        Set.of("t53_8264_019", "EVB-P15079256")
                )
        );
    }

    private List<IntervalRule.ModelRule> createModelRules() {
        return List.of(
                new IntervalRule.ModelRule(
                        Duration.ofSeconds(60),
                        "Alfen BV",
                        "NG920-5250[6-9]"
                ),
                new IntervalRule.ModelRule(
                        Duration.ofSeconds(120),
                        "ChargeStorm AB",
                        "Chargestorm Connected"
                )
        );
    }

    private IntervalRule.DefaultRule createDefaultRule() {
        return new IntervalRule.DefaultRule(Duration.ofSeconds(1800));
    }
}
