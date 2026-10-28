package faang.school.notificationservice.metrics;

import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class EmailNotificationMetricsTest {

    private MockClock clock;
    private SimpleMeterRegistry meterRegistry;
    private EmailNotificationMetrics metrics;

    @BeforeEach
    void setUp() {
        clock = new MockClock();
        meterRegistry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);
        metrics = new EmailNotificationMetrics(meterRegistry);
    }

    @AfterEach
    void tearDown() {
        meterRegistry.close();
    }

    @Test
    void shouldRegisterSeparateTimersWithoutRecordingOperations() {
        assertThat(successTimer().count()).isZero();
        assertThat(successTimer().totalTime(TimeUnit.MILLISECONDS)).isZero();
        assertThat(errorTimer().count()).isZero();
        assertThat(errorTimer().totalTime(TimeUnit.MILLISECONDS)).isZero();
    }

    @Test
    void shouldNotRecordOperationUntilSampleIsStopped() {
        metrics.start();
        clock.add(Duration.ofMillis(100));

        assertThat(successTimer().count()).isZero();
        assertThat(errorTimer().count()).isZero();
    }

    @Test
    void shouldRecordSuccessfulOperationsAndTheirTotalDuration() {
        Timer.Sample firstSample = metrics.start();
        clock.add(Duration.ofMillis(100));
        metrics.recordSuccess(firstSample);

        Timer.Sample secondSample = metrics.start();
        clock.add(Duration.ofMillis(150));
        metrics.recordSuccess(secondSample);

        assertThat(successTimer().count()).isEqualTo(2);
        assertThat(successTimer().totalTime(TimeUnit.MILLISECONDS)).isEqualTo(250.0);
        assertThat(errorTimer().count()).isZero();
    }

    @Test
    void shouldRecordFailedOperationWithoutRecordingSuccess() {
        Timer.Sample sample = metrics.start();
        clock.add(Duration.ofMillis(75));

        metrics.recordError(sample);

        assertThat(errorTimer().count()).isEqualTo(1);
        assertThat(errorTimer().totalTime(TimeUnit.MILLISECONDS)).isEqualTo(75.0);
        assertThat(successTimer().count()).isZero();
    }

    @Test
    void shouldKeepOverlappingSamplesIndependent() {
        Timer.Sample firstSample = metrics.start();
        clock.add(Duration.ofMillis(20));
        Timer.Sample secondSample = metrics.start();
        clock.add(Duration.ofMillis(30));
        metrics.recordSuccess(firstSample);
        clock.add(Duration.ofMillis(10));
        metrics.recordError(secondSample);

        assertThat(successTimer().count()).isEqualTo(1);
        assertThat(successTimer().totalTime(TimeUnit.MILLISECONDS)).isEqualTo(50.0);
        assertThat(errorTimer().count()).isEqualTo(1);
        assertThat(errorTimer().totalTime(TimeUnit.MILLISECONDS)).isEqualTo(40.0);
    }

    private Timer successTimer() {
        return meterRegistry.get("notifications.email.send")
                .tag("outcome", "success")
                .timer();
    }

    private Timer errorTimer() {
        return meterRegistry.get("notifications.email.send")
                .tag("outcome", "error")
                .timer();
    }
}
