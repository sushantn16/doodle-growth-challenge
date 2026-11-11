package io.github.sushantn16.dashboard.experiment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VisitorCookieTest {

    @Test
    void acceptsTheIdsItIssues() {
        assertThat(VisitorCookie.isValidId(VisitorCookie.newId())).isTrue();
    }

    @Test
    void rejectsAnythingElse() {
        assertThat(VisitorCookie.isValidId(null)).isFalse();
        assertThat(VisitorCookie.isValidId("")).isFalse();
        assertThat(VisitorCookie.isValidId("not-a-uuid")).isFalse();
        // The same UUID in uppercase is a different string, so it would be a second map key.
        assertThat(VisitorCookie.isValidId("1B4E28BA-2FA1-11D2-883F-0016D3CCA427")).isFalse();
    }
}
