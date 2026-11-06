package nl.openvalue.distributor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
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
    void bindsToTheLoopbackAddressOnly() {
        assertThat(serverAddress).isEqualTo("127.0.0.1");
    }
}
