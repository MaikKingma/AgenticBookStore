package nl.openvalue.distributor.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import nl.openvalue.distributor.catalog.TitleCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** The chaos knobs wired into the web slice with every call set to fail. */
@WebMvcTest(value = TitlesController.class,
        properties = {"distributor.chaos.failure-rate=1.0", "distributor.chaos.latency-ms=5"})
class ChaosApiTest {

    @TestConfiguration
    static class FixtureCatalogue {

        @Bean
        TitleCatalog titleCatalog() {
            return ApiTestFixtures.catalog();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChaosProperties chaosProperties;

    @Test
    void apiCallsReturn503TemporarilyUnavailableEnvelope() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(mockMvc.perform(get("/api/titles")), 503, "TEMPORARILY_UNAVAILABLE");
        ApiTestFixtures.expectErrorEnvelope(
                mockMvc.perform(get("/api/titles/" + ApiTestFixtures.IN_STOCK_EAN)), 503, "TEMPORARILY_UNAVAILABLE");
        ApiTestFixtures.expectErrorEnvelope(mockMvc.perform(get("/api/subjects")), 503, "TEMPORARILY_UNAVAILABLE");
    }

    @Test
    void theChaosEnvelopeIsApplicationJsonEvenWhenProblemJsonIsAccepted() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(
                mockMvc.perform(get("/api/titles").accept(MediaType.APPLICATION_PROBLEM_JSON)), 503,
                "TEMPORARILY_UNAVAILABLE");
    }

    @Test
    void nonApiPathsAreNotAffected() throws Exception {
        mockMvc.perform(get("/elsewhere")).andExpect(status().isNotFound());
        mockMvc.perform(get("/apiary")).andExpect(status().isNotFound());
    }

    @Test
    void chaosPropertiesBindFromTheDocumentedKeys() {
        assertThat(chaosProperties).isEqualTo(new ChaosProperties(5, 1.0));
    }
}
