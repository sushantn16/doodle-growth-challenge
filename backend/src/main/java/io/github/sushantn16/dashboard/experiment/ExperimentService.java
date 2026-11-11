package io.github.sushantn16.dashboard.experiment;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** The rules of the Upgrade button experiment: who sees which version, and which events count. */
@Service
public class ExperimentService {

    private final VariantAssigner assigner;
    private final EventTracker tracker;

    public ExperimentService(VariantAssigner assigner, EventTracker tracker) {
        this.assigner = assigner;
        this.tracker = tracker;
    }

    /** The visitor's version. A new visitor gets one at random; after that it never changes. */
    public Variant assign(String visitorId) {
        return assigner.assign(visitorId);
    }

    /**
     * Records an event if {@code shown}, the version the browser showed, is the visitor's
     * assignment. So an event can never land in a version the visitor didn't see, for example
     * when a brand-new visitor opens two tabs at once and the browser keeps only one cookie.
     *
     * @param visitorId the visitor's id, or null if the request had none
     * @return whether the event was counted. It isn't if the visitor is unknown (for example, the
     *     server restarted since the page loaded) or was assigned a different version.
     */
    public boolean record(String visitorId, EventType type, Variant shown) {
        Optional<Variant> assigned = visitorId == null ? Optional.empty() : assigner.find(visitorId);
        if (assigned.isEmpty() || assigned.get() != shown) {
            return false;
        }
        switch (type) {
            case VIEW -> tracker.recordView(visitorId, shown);
            case CLICK -> tracker.recordClick(visitorId, shown);
        }
        return true;
    }

    public List<VariantResult> results() {
        return tracker.results();
    }
}
