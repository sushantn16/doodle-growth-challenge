package com.example.dashboard.experiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.dashboard.experiment.ExperimentController.Assignment;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // fresh in-memory state per test
class ExperimentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper json;

    @Test
    void givesANewVisitorAnIdCookieAndAVariant() throws Exception {
        mockMvc.perform(get("/api/experiment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variant").isString())
                .andExpect(cookie().exists(ExperimentController.VISITOR_COOKIE))
                .andExpect(cookie().httpOnly(ExperimentController.VISITOR_COOKIE, true))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void keepsAReturningVisitorInTheSameVariant() throws Exception {
        MvcResult first = mockMvc.perform(get("/api/experiment")).andReturn();
        Cookie visitor = first.getResponse().getCookie(ExperimentController.VISITOR_COOKIE);
        Variant variant = variantOf(first);

        for (int i = 0; i < 20; i++) {
            MvcResult again = mockMvc.perform(get("/api/experiment").cookie(visitor))
                    .andExpect(cookie().doesNotExist(ExperimentController.VISITOR_COOKIE))
                    .andReturn();
            assertThat(variantOf(again)).isEqualTo(variant);
        }
    }

    @Test
    void replacesAVisitorIdTheServerCouldNotHaveIssued() throws Exception {
        mockMvc.perform(get("/api/experiment").cookie(new Cookie(ExperimentController.VISITOR_COOKIE, "not-a-uuid")))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(ExperimentController.VISITOR_COOKIE));
    }

    @Test
    void splitsNewVisitorsAboutHalfAndHalf() throws Exception {
        int visitors = 2_000;
        int inA = 0;
        for (int i = 0; i < visitors; i++) {
            if (variantOf(mockMvc.perform(get("/api/experiment")).andReturn()) == Variant.A) {
                inA++;
            }
        }
        // One standard deviation is sqrt(2_000 * 0.25) ≈ 22, so this allows about 4 of them.
        assertThat(inA).isBetween(910, 1_090);
    }

    private Variant variantOf(MvcResult result) throws Exception {
        return json.readValue(result.getResponse().getContentAsString(), Assignment.class).variant();
    }
}
