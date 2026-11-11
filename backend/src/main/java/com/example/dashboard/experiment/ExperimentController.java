package com.example.dashboard.experiment;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/experiment")
class ExperimentController {

    static final String VISITOR_COOKIE = "visitor_id";
    private static final Duration COOKIE_LIFETIME = Duration.ofDays(365);

    private final VariantAssigner assigner;
    private final EventTracker tracker;

    ExperimentController(VariantAssigner assigner, EventTracker tracker) {
        this.assigner = assigner;
        this.tracker = tracker;
    }

    record Assignment(Variant variant) {
    }

    enum EventType {
        VIEW,
        CLICK
    }

    record Event(EventType type) {
    }

    record Results(List<VariantResult> variants) {
    }

    /**
     * Returns the visitor's variant. A visitor without a valid id cookie gets a new id, set as an
     * HttpOnly cookie, so the browser never has to create or store the id itself.
     */
    @GetMapping
    ResponseEntity<Assignment> assignment(@CookieValue(name = VISITOR_COOKIE, required = false) String visitorId) {
        boolean newVisitor = !isValidVisitorId(visitorId);
        String id = newVisitor ? UUID.randomUUID().toString() : visitorId;

        var response = ResponseEntity.ok().cacheControl(CacheControl.noStore());
        if (newVisitor) {
            response.header(HttpHeaders.SET_COOKIE, visitorCookie(id).toString());
        }
        return response.body(new Assignment(assigner.assign(id)));
    }

    /**
     * Records that the visitor saw or clicked the button. The variant is looked up on the server, so
     * a client can't record events for a variant it wasn't shown. 409 if the visitor has no
     * assignment, for example because the server restarted since the page loaded.
     */
    @PostMapping("/events")
    ResponseEntity<Void> recordEvent(
            @CookieValue(name = VISITOR_COOKIE, required = false) String visitorId, @RequestBody Event event) {
        if (event.type() == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<Variant> variant = visitorId == null ? Optional.empty() : assigner.find(visitorId);
        if (variant.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        switch (event.type()) {
            case VIEW -> tracker.recordView(visitorId, variant.get());
            case CLICK -> tracker.recordClick(visitorId, variant.get());
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/results")
    ResponseEntity<Results> results() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Results(tracker.results()));
    }

    private static ResponseCookie visitorCookie(String visitorId) {
        return ResponseCookie.from(VISITOR_COOKIE, visitorId)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(COOKIE_LIFETIME)
                .build();
    }

    /** Only ids this server could have issued are accepted, so a client can't fill memory with junk ids. */
    static boolean isValidVisitorId(String visitorId) {
        if (visitorId == null) {
            return false;
        }
        try {
            return UUID.fromString(visitorId).toString().equals(visitorId);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
