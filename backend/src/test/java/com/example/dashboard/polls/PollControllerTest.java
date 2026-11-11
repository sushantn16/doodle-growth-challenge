package com.example.dashboard.polls;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PollControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void listsAllPollsNewestFirstWithTitleAndCreationDate() throws Exception {
        mockMvc.perform(get("/api/polls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(36)))
                // The newest poll in polls.json. It is "hidden", which the dashboard ignores.
                .andExpect(jsonPath("$[0].id").value("xsd4cv89t5f5um4b"))
                .andExpect(jsonPath("$[0].title").value("Qui sont les superhéros Marvel les plus oufs?"))
                .andExpect(jsonPath("$[0].createdAt").value("2017-01-27T12:52:49.056Z"))
                // The oldest poll.
                .andExpect(jsonPath("$[35].id").value("yqca2hi5yrpkrid5"));
    }
}
