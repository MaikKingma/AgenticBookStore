package nl.openvalue.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.EntityExchangeResult;

/**
 * The storefront entry points on the real server: the root redirect, the two forwards to the single page, and proof
 * that the REST API and the health endpoint are left alone. The client does not follow redirects, so the 302 itself
 * is what is observed.
 */
class StorefrontConfigTest extends IntegrationTestBase {

    @Test
    void rootRedirectsToShop() {
        EntityExchangeResult<String> result = restTestClient.get().uri("/").exchange()
                .expectStatus().isEqualTo(302)
                .expectBody(String.class).returnResult();

        String location = result.getResponseHeaders().getFirst(HttpHeaders.LOCATION);
        assertThat(location).isNotNull();
        URI requested = result.getUrl();
        assertThat(requested.resolve(location)).isEqualTo(requested.resolve("/shop/"));
    }

    @Test
    void shopWithSlashServesIndexHtml() {
        assertServesIndexHtml("/shop/");
    }

    @Test
    void shopWithoutSlashServesIndexHtml() {
        assertServesIndexHtml("/shop");
    }

    @Test
    void booksIsNotRedirected() {
        restTestClient.get().uri("/books").exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectHeader().doesNotExist(HttpHeaders.LOCATION);
    }

    @Test
    void healthIsNotRedirected() {
        restTestClient.get().uri("/actuator/health").exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist(HttpHeaders.LOCATION)
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    private void assertServesIndexHtml(String path) {
        EntityExchangeResult<String> result = restTestClient.get().uri(path).exchange()
                .expectStatus().isEqualTo(200)
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class).returnResult();

        assertThat(result.getResponseBody()).contains("id=\"root\"");
    }
}
