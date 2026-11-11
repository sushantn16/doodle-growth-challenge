package io.github.sushantn16.dashboard.experiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.sushantn16.dashboard.experiment.ExperimentController.Assignment;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
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
                .andExpect(cookie().exists(VisitorCookie.NAME))
                .andExpect(cookie().httpOnly(VisitorCookie.NAME, true))
                .andExpect(cookie().sameSite(VisitorCookie.NAME, "Lax"))
                .andExpect(cookie().path(VisitorCookie.NAME, "/"))
                .andExpect(cookie().maxAge(VisitorCookie.NAME, 365 * 24 * 60 * 60))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void keepsAReturningVisitorInTheSameVariant() throws Exception {
        MvcResult first = mockMvc.perform(get("/api/experiment")).andReturn();
        Cookie visitor = visitorCookie(first);
        Variant variant = variantOf(first);

        for (int i = 0; i < 20; i++) {
            MvcResult again = mockMvc.perform(get("/api/experiment").cookie(visitor))
                    .andExpect(cookie().doesNotExist(VisitorCookie.NAME))
                    .andReturn();
            assertThat(variantOf(again)).isEqualTo(variant);
        }
    }

    @Test
    void replacesAVisitorIdTheServerCouldNotHaveIssued() throws Exception {
        mockMvc.perform(get("/api/experiment").cookie(new Cookie(VisitorCookie.NAME, "not-a-uuid")))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(VisitorCookie.NAME));
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

    @Test
    void reportsZeroForBothVariantsBeforeAnyoneVisits() throws Exception {
        mockMvc.perform(get("/api/experiment/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variants[0].variant").value("A"))
                .andExpect(jsonPath("$.variants[0].saw").value(0))
                .andExpect(jsonPath("$.variants[0].clicked").value(0))
                .andExpect(jsonPath("$.variants[0].clickRate").value(0.0))
                .andExpect(jsonPath("$.variants[1].variant").value("B"))
                .andExpect(jsonPath("$.variants[1].saw").value(0));
    }

    @Test
    void countsEachVisitorOnceInTheirOwnVariant() throws Exception {
        // Three visitors load the page. The first reloads twice; the second clicks three times.
        MvcResult[] visits = {
            mockMvc.perform(get("/api/experiment")).andReturn(),
            mockMvc.perform(get("/api/experiment")).andReturn(),
            mockMvc.perform(get("/api/experiment")).andReturn(),
        };
        for (MvcResult visit : visits) {
            send(visit, "VIEW");
        }
        send(visits[0], "VIEW");
        send(visits[0], "VIEW");
        send(visits[1], "CLICK");
        send(visits[1], "CLICK");
        send(visits[1], "CLICK");

        // Which variant each visitor landed in is random, so work out the expected counts from it.
        int[] saw = new int[2];
        int[] clicked = new int[2];
        for (MvcResult visit : visits) {
            saw[variantOf(visit).ordinal()]++;
        }
        clicked[variantOf(visits[1]).ordinal()]++;

        var results = mockMvc.perform(get("/api/experiment/results")).andExpect(status().isOk());
        for (Variant variant : Variant.values()) {
            int i = variant.ordinal();
            results.andExpect(jsonPath("$.variants[%d].variant", i).value(variant.name()))
                    .andExpect(jsonPath("$.variants[%d].saw", i).value(saw[i]))
                    .andExpect(jsonPath("$.variants[%d].clicked", i).value(clicked[i]))
                    .andExpect(jsonPath("$.variants[%d].clickRate", i).value(saw[i] == 0 ? 0.0 : (double) clicked[i] / saw[i]));
        }
    }

    @Test
    void rejectsAnEventFromAVisitorWithoutAnAssignment() throws Exception {
        postEvent(null, body("CLICK", Variant.A)).andExpect(status().isConflict());

        Cookie unknown = new Cookie(VisitorCookie.NAME, "1b4e28ba-2fa1-11d2-883f-0016d3cca427");
        postEvent(unknown, body("VIEW", Variant.A)).andExpect(status().isConflict());
    }

    @Test
    void neverCountsAnEventForAVariantTheVisitorWasNotAssigned() throws Exception {
        MvcResult visit = mockMvc.perform(get("/api/experiment")).andReturn();
        Variant other = variantOf(visit) == Variant.A ? Variant.B : Variant.A;

        postEvent(visitorCookie(visit), body("CLICK", other)).andExpect(status().isConflict());

        mockMvc.perform(get("/api/experiment/results"))
                .andExpect(jsonPath("$.variants[0].saw").value(0))
                .andExpect(jsonPath("$.variants[1].saw").value(0));
    }

    @Test
    void rejectsAnEventWithoutAKnownTypeOrVariant() throws Exception {
        Cookie visitor = visitorCookie(mockMvc.perform(get("/api/experiment")).andReturn());

        postEvent(visitor, "{}").andExpect(status().isBadRequest());
        postEvent(visitor, "{\"type\":\"VIEW\"}").andExpect(status().isBadRequest());
        postEvent(visitor, "{\"type\":\"HOVER\",\"variant\":\"A\"}").andExpect(status().isBadRequest());
        // Jackson would otherwise read 1 as the second constant, CLICK.
        postEvent(visitor, "{\"type\":1,\"variant\":\"A\"}").andExpect(status().isBadRequest());
    }

    private void send(MvcResult visit, String type) throws Exception {
        postEvent(visitorCookie(visit), body(type, variantOf(visit))).andExpect(status().isNoContent());
    }

    private static String body(String type, Variant variant) {
        return "{\"type\":\"" + type + "\",\"variant\":\"" + variant + "\"}";
    }

    private ResultActions postEvent(Cookie visitor, String body) throws Exception {
        var request = post("/api/experiment/events").contentType(MediaType.APPLICATION_JSON).content(body);
        if (visitor != null) {
            request.cookie(visitor);
        }
        return mockMvc.perform(request);
    }

    private static Cookie visitorCookie(MvcResult visit) {
        return visit.getResponse().getCookie(VisitorCookie.NAME);
    }

    private Variant variantOf(MvcResult result) throws Exception {
        return json.readValue(result.getResponse().getContentAsString(), Assignment.class).variant();
    }
}
