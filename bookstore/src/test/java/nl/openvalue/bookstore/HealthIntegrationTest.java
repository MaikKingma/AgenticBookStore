package nl.openvalue.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

class HealthIntegrationTest extends IntegrationTestBase {

    @Value("${server.address}")
    private String serverAddress;

    @Test
    void healthReportsTheApplicationAndTheDatabaseUp() {
        restTestClient.get().uri("/actuator/health").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.components.db.status").isEqualTo("UP");
    }

    @Test
    void bindsToTheLoopbackAddressOnly() {
        assertThat(serverAddress).isEqualTo("127.0.0.1");
    }
}
