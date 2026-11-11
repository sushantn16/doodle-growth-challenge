package io.github.sushantn16.dashboard.experiment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class VariantAssignerTest {

    private final VariantAssigner assigner = new VariantAssigner(new Random(42));

    @Test
    void splitsVisitorsAboutHalfAndHalf() {
        int visitors = 10_000;
        int inA = 0;
        for (int i = 0; i < visitors; i++) {
            if (assigner.assign("visitor-" + i) == Variant.A) {
                inA++;
            }
        }
        // One standard deviation is sqrt(10_000 * 0.5 * 0.5) = 50, so ±200 is 4 standard deviations:
        // a fair coin falls outside this range about once in 16,000 runs.
        assertThat(inA).isBetween(4_800, 5_200);
    }

    @Test
    void keepsAVisitorInTheSameVariant() {
        Variant first = assigner.assign("visitor-1");
        for (int i = 0; i < 100; i++) {
            assertThat(assigner.assign("visitor-1")).isEqualTo(first);
        }
    }

    @Test
    void findDoesNotAssign() {
        assertThat(assigner.find("visitor-1")).isEmpty();
        Variant assigned = assigner.assign("visitor-1");
        assertThat(assigner.find("visitor-1")).contains(assigned);
    }

    @Test
    void givesSimultaneousRequestsFromOneVisitorTheSameVariant() throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(16)) {
            for (int round = 0; round < 200; round++) {
                String visitor = "visitor-" + round;
                List<Future<Variant>> answers = new ArrayList<>();
                for (int i = 0; i < 16; i++) {
                    answers.add(pool.submit(() -> assigner.assign(visitor)));
                }
                Set<Variant> distinct = new HashSet<>();
                for (Future<Variant> answer : answers) {
                    distinct.add(answer.get());
                }
                assertThat(distinct).hasSize(1);
            }
        }
    }
}
