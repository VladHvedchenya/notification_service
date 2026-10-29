package faang.school.notificationservice.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Gauge;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class EmailNotificationMetrics {

    private final MeterRegistry meterRegistry;
    private final Timer successTimer;
    private final Timer errorTimer;
    private final AtomicInteger activeSends = new AtomicInteger();
    private final AtomicInteger peakSends = new AtomicInteger();

    public EmailNotificationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.successTimer = Timer.builder("notifications.email.send")
                .tag("outcome", "success")
                .register(meterRegistry);

        this.errorTimer = Timer.builder("notifications.email.send")
                .tag("outcome", "error")
                .register(meterRegistry);
        Gauge.builder("notifications.email.active", activeSends, AtomicInteger::get)
                .register(meterRegistry);

        Gauge.builder("notifications.email.peak", peakSends, AtomicInteger::get)
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

    public void sendingStarted() {
        int activeCount = activeSends.incrementAndGet();
        peakSends.accumulateAndGet(activeCount, Math::max);
    }

    public void sendingFinished() {
        activeSends.decrementAndGet();
    }
}
