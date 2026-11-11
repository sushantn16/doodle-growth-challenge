package io.github.sushantn16.dashboard.experiment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.Test;

class ExperimentServiceTest {

    private final EventTracker tracker = new EventTracker();
    private final ExperimentService experiment = new ExperimentService(new VariantAssigner(new Random(42)), tracker);

    @Test
    void countsEventsForTheVersionTheVisitorWasAssigned() {
        Variant assigned = experiment.assign("visitor-1");

        assertThat(experiment.record("visitor-1", EventType.VIEW, assigned)).isTrue();
        assertThat(experiment.record("visitor-1", EventType.CLICK, assigned)).isTrue();

        assertThat(experiment.results()).contains(new VariantResult(assigned, 1, 1, 1.0));
    }

    @Test
    void doesNotCountAnEventForAnotherVersion() {
        Variant other = experiment.assign("visitor-1") == Variant.A ? Variant.B : Variant.A;

        assertThat(experiment.record("visitor-1", EventType.CLICK, other)).isFalse();

        assertThat(experiment.results()).allSatisfy(result -> assertThat(result.saw()).isZero());
    }

    @Test
    void doesNotCountAnEventFromAnUnknownVisitor() {
        assertThat(experiment.record("never-assigned", EventType.VIEW, Variant.A)).isFalse();
        assertThat(experiment.record(null, EventType.VIEW, Variant.A)).isFalse();

        assertThat(experiment.results()).allSatisfy(result -> assertThat(result.saw()).isZero());
    }
}
