package io.github.sushantn16.dashboard.polls;

import java.time.Instant;

/** The part of a poll the dashboard shows. */
public record Poll(String id, String title, Instant createdAt) {}
