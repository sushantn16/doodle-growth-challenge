package com.example.dashboard.polls;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/polls")
class PollController {

    private final PollRepository polls;

    PollController(PollRepository polls) {
        this.polls = polls;
    }

    @GetMapping
    List<Poll> list() {
        return polls.findAllNewestFirst();
    }
}
