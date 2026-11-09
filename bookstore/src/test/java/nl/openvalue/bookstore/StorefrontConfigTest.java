package nl.openvalue.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest
    @ValueSource(strings = {"/shop", "/shop/", "/shop/index.html"})
    void shopEntryDocumentMustBeRevalidated(String path) {
        EntityExchangeResult<String> result = restTestClient.get().uri(path).exchange()
                .expectStatus().isEqualTo(200)
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectHeader().exists(HttpHeaders.CACHE_CONTROL)
                .expectBody(String.class).returnResult();

        assertThat(result.getResponseBody()).contains("id=\"root\"");
        String cacheControl = result.getResponseHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
        assertThat(Arrays.stream(cacheControl.split(",")).map(String::trim))
                .as("directives of Cache-Control '%s'", cacheControl)
                .anyMatch("no-cache"::equalsIgnoreCase);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/shop", "/shop/", "/shop/index.html"})
    void shopEntryDocumentIgnoresDateValidators(String path) {
        String lastModified = restTestClient.get().uri(path).exchange()
                .expectStatus().isEqualTo(200)
                .expectBody(String.class).returnResult()
                .getResponseHeaders().getFirst(HttpHeaders.LAST_MODIFIED);

        assertIsServedAgainDespiteValidator(path, formatHttpDate(Instant.now()));
        if (lastModified != null) {
            assertIsServedAgainDespiteValidator(path, lastModified);
        }
    }

    @Test
    void hashedAssetsAreNotMarkedForRevalidation() {
        EntityExchangeResult<String> result = restTestClient.get().uri("/shop/assets/app-0a1b2c3d.js").exchange()
                .expectStatus().isEqualTo(200)
                .expectBody(String.class).returnResult();

        String cacheControl = result.getResponseHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
        if (cacheControl != null) {
            assertThat(cacheControl.toLowerCase()).doesNotContain("no-cache");
        }
    }

    private void assertIsServedAgainDespiteValidator(String path, String ifModifiedSince) {
        EntityExchangeResult<String> result = restTestClient.get().uri(path)
                .header(HttpHeaders.IF_MODIFIED_SINCE, ifModifiedSince)
                .exchange()
                .expectStatus().isEqualTo(200)
                .expectBody(String.class).returnResult();

        assertThat(result.getResponseBody()).contains("id=\"root\"");
    }

    private static String formatHttpDate(Instant instant) {
        HttpHeaders headers = new HttpHeaders();
        headers.setIfModifiedSince(instant);
        return headers.getFirst(HttpHeaders.IF_MODIFIED_SINCE);
    }
}
