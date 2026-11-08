package nl.openvalue.distributor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(FixedClockTestConfiguration.class)
class HealthEndpointTest {

    @Autowired
    private RestTestClient restTestClient;

    @Value("${server.address}")
    private String serverAddress;

    @Test
    void healthReportsTheApplicationUp() {
        restTestClient.get().uri("/actuator/health").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void wrongMethodOnActuatorKeepsSpringBootsDefaultBody() {
        // Outside /api/ the envelope handler rethrows, so Boot answers as before: no errorCode in the body.
        restTestClient.post().uri("/actuator/health").exchange()
                .expectStatus().isEqualTo(405)
                .expectHeader().valueEquals("Allow", "GET")
                .expectBody()
                .jsonPath("$.status").isEqualTo(405)
                .jsonPath("$.errorCode").doesNotExist();
    }

    @Test
    void unknownApiPathKeepsSpringBootsDefault404Body() {
        // The accepted known limit of spec 05: an unmapped /api/ path is not an envelope error.
        restTestClient.get().uri("/api/no-such-resource").exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.errorCode").doesNotExist();
    }

    @Test
    void bindsToTheLoopbackAddressOnly() {
        assertThat(serverAddress).isEqualTo("127.0.0.1");
    }
}
