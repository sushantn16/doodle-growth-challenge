package io.github.sushantn16.dashboard.experiment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The experiment's HTTP API. The rules live in {@link ExperimentService}. */
@RestController
@RequestMapping("/api/experiment")
class ExperimentController {

    private final ExperimentService experiment;

    ExperimentController(ExperimentService experiment) {
        this.experiment = experiment;
    }

    record Assignment(Variant variant) {}

    /** {@code variant} is the version the browser showed. */
    record Event(@NotNull EventType type, @NotNull Variant variant) {}

    record Results(List<VariantResult> variants) {}

    /** The visitor's version. A visitor without a valid id cookie gets a new one. */
    @GetMapping
    ResponseEntity<Assignment> assignment(@CookieValue(name = VisitorCookie.NAME, required = false) String visitorId) {
        boolean newVisitor = !VisitorCookie.isValidId(visitorId);
        String id = newVisitor ? VisitorCookie.newId() : visitorId;

        var response = ResponseEntity.ok().cacheControl(CacheControl.noStore());
        if (newVisitor) {
            response.header(HttpHeaders.SET_COOKIE, VisitorCookie.create(id).toString());
        }
        return response.body(new Assignment(experiment.assign(id)));
    }

    /**
     * 204 if the event was counted, 409 if the visitor is unknown or was assigned another version.
     * A missing or unknown type or variant is a 400, from {@code @Valid}.
     */
    @PostMapping("/events")
    ResponseEntity<Void> recordEvent(
            @CookieValue(name = VisitorCookie.NAME, required = false) String visitorId,
            @Valid @RequestBody Event event) {
        boolean counted = experiment.record(visitorId, event.type(), event.variant());
        return counted
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @GetMapping("/results")
    ResponseEntity<Results> results() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Results(experiment.results()));
    }
}
