package com.example.dashboard.experiment;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Remembers which visitors saw and clicked the button, per variant. Storing visitor ids in sets is
 * what makes a reload or a second click count once. In memory only.
 *
 * <p>The methods are synchronized so the counts and the click rate in a result always belong
 * together. The traffic is small, so the lock costs nothing that matters.
 */
@Component
public class EventTracker {

    private final Map<Variant, Set<String>> viewers = new EnumMap<>(Variant.class);
    private final Map<Variant, Set<String>> clickers = new EnumMap<>(Variant.class);

    public EventTracker() {
        for (Variant variant : Variant.values()) {
            viewers.put(variant, new HashSet<>());
            clickers.put(variant, new HashSet<>());
        }
    }

    public synchronized void recordView(String visitorId, Variant variant) {
        viewers.get(variant).add(visitorId);
    }

    /**
     * A visitor who clicked has seen the button, so a click also counts as a view, even if the
     * view event was lost. That keeps "clicked" at or below "saw".
     */
    public synchronized void recordClick(String visitorId, Variant variant) {
        viewers.get(variant).add(visitorId);
        clickers.get(variant).add(visitorId);
    }

    /** One result per variant, including variants nobody has seen yet. */
    public synchronized List<VariantResult> results() {
        return Arrays.stream(Variant.values())
                .map(variant -> VariantResult.of(variant, viewers.get(variant).size(), clickers.get(variant).size()))
                .toList();
    }
}
