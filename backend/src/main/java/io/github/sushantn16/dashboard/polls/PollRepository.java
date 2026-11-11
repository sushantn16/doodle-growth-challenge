package io.github.sushantn16.dashboard.polls;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Loads the polls once at startup and keeps them in memory, newest first. */
@Repository
public class PollRepository {

    private final List<Poll> newestFirst;

    public PollRepository(JsonMapper jsonMapper, @Value("classpath:polls.json") Resource source) throws IOException {
        try (InputStream in = source.getInputStream()) {
            newestFirst = Arrays.stream(jsonMapper.readValue(in, PollJson[].class))
                    .map(PollJson::toPoll)
                    .sorted(Comparator.comparing(Poll::createdAt).reversed())
                    .toList();
        }
    }

    public List<Poll> findAllNewestFirst() {
        return newestFirst;
    }

    /** One entry of polls.json. Every field the dashboard doesn't need, including "hidden", is ignored. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record PollJson(String id, String title, long initiated) {

        Poll toPoll() {
            return new Poll(id, title, Instant.ofEpochMilli(initiated));
        }
    }
}
