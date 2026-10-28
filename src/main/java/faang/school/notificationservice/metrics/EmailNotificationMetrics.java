package faang.school.notificationservice.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationMetrics {

    private final MeterRegistry meterRegistry;
    private final Timer successTimer;
    private final Timer errorTimer;

    public EmailNotificationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.successTimer = Timer.builder("notifications.email.send")
                .tag("outcome", "success")
                .register(meterRegistry);

        this.errorTimer = Timer.builder("notifications.email.send")
                .tag("outcome", "error")
                .register(meterRegistry);
    }

    public Timer.Sample start() {
        return Timer.start(meterRegistry);
    }

    public void recordSuccess(Timer.Sample sample) {
        sample.stop(successTimer);
    }

    public void recordError(Timer.Sample sample) {
        sample.stop(errorTimer);
    }
}
