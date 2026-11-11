package io.github.sushantn16.dashboard.experiment;

import java.time.Duration;
import java.util.UUID;
import org.springframework.http.ResponseCookie;

/**
 * The cookie that identifies a visitor. The server creates the id, and the cookie is HttpOnly, so
 * the browser never has to create, store or read the id itself.
 */
final class VisitorCookie {

    static final String NAME = "visitor_id";
    private static final Duration LIFETIME = Duration.ofDays(365);

    private VisitorCookie() {}

    static String newId() {
        return UUID.randomUUID().toString();
    }

    static ResponseCookie create(String visitorId) {
        return ResponseCookie.from(NAME, visitorId)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(LIFETIME)
                .build();
    }

    /**
     * Accepts only ids in the form this server issues (a lowercase UUID), so arbitrary cookie values,
     * such as very long strings, never become map keys. This doesn't limit how many visitors a client
     * can create: every request without a cookie gets a new id. See "What I left out" in the README.
     */
    static boolean isValidId(String visitorId) {
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
