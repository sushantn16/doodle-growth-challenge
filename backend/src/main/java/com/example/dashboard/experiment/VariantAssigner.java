package com.example.dashboard.experiment;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Component;

/** Puts each visitor in A or B with a 50/50 chance, and keeps them there. In memory only. */
@Component
public class VariantAssigner {

    private final Map<String, Variant> assignments = new ConcurrentHashMap<>();
    private final RandomGenerator random;

    public VariantAssigner(RandomGenerator random) {
        this.random = random;
    }

    /** Returns the visitor's variant. A new visitor gets one at random; after that it never changes. */
    public Variant assign(String visitorId) {
        return assignments.computeIfAbsent(visitorId, id -> random.nextBoolean() ? Variant.A : Variant.B);
    }

    /** The visitor's variant, if they have been assigned one. Never assigns. */
    public Optional<Variant> find(String visitorId) {
        return Optional.ofNullable(assignments.get(visitorId));
    }
}
