package com.example.dashboard.experiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class EventTrackerTest {

    private final EventTracker tracker = new EventTracker();

    @Test
    void reportsBothVariantsBeforeAnyoneVisits() {
        assertThat(tracker.results()).containsExactly(
                new VariantResult(Variant.A, 0, 0, 0),
                new VariantResult(Variant.B, 0, 0, 0));
    }

    @Test
    void countsAVisitorWhoReloadsOnce() {
        tracker.recordView("visitor-1", Variant.A);
        tracker.recordView("visitor-1", Variant.A);
        tracker.recordView("visitor-1", Variant.A);

        assertThat(result(Variant.A)).isEqualTo(new VariantResult(Variant.A, 1, 0, 0));
    }

    @Test
    void countsAVisitorWhoClicksSeveralTimesOnce() {
        tracker.recordView("visitor-1", Variant.A);
        tracker.recordClick("visitor-1", Variant.A);
        tracker.recordClick("visitor-1", Variant.A);

        assertThat(result(Variant.A)).isEqualTo(new VariantResult(Variant.A, 1, 1, 1.0));
    }

    @Test
    void dividesVisitorsWhoClickedByVisitorsWhoSaw() {
        tracker.recordView("visitor-1", Variant.B);
        tracker.recordView("visitor-2", Variant.B);
        tracker.recordView("visitor-3", Variant.B);
        tracker.recordClick("visitor-2", Variant.B);

        VariantResult b = result(Variant.B);
        assertThat(b.saw()).isEqualTo(3);
        assertThat(b.clicked()).isEqualTo(1);
        assertThat(b.clickRate()).isCloseTo(1.0 / 3, within(1e-9));
    }

    @Test
    void countsAClickWithoutAViewAsAView() {
        tracker.recordClick("visitor-1", Variant.A);

        assertThat(result(Variant.A)).isEqualTo(new VariantResult(Variant.A, 1, 1, 1.0));
    }

    @Test
    void keepsTheVariantsApart() {
        tracker.recordView("visitor-1", Variant.A);
        tracker.recordView("visitor-2", Variant.B);
        tracker.recordClick("visitor-2", Variant.B);

        assertThat(tracker.results()).containsExactly(
                new VariantResult(Variant.A, 1, 0, 0),
                new VariantResult(Variant.B, 1, 1, 1.0));
    }

    @Test
    void countsExactlyWhenEventsArriveAtTheSameTime() {
        try (ExecutorService pool = Executors.newFixedThreadPool(16)) {
            for (int i = 0; i < 1_000; i++) {
                String visitor = "visitor-" + i;
                for (int view = 0; view < 3; view++) {
                    pool.submit(() -> tracker.recordView(visitor, Variant.A));
                }
                if (i % 2 == 0) {
                    pool.submit(() -> tracker.recordClick(visitor, Variant.A));
                    pool.submit(() -> tracker.recordClick(visitor, Variant.A));
                }
            }
        } // close() waits for every task

        assertThat(result(Variant.A)).isEqualTo(new VariantResult(Variant.A, 1_000, 500, 0.5));
    }

    private VariantResult result(Variant variant) {
        return tracker.results().stream().filter(r -> r.variant() == variant).findFirst().orElseThrow();
    }
}
