package nl.openvalue.bookstore;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Entry points of the storefront, a single page application served from {@code static/shop}: the root of the site
 * redirects to {@code /shop/}, and both {@code /shop} and {@code /shop/} show the page's {@code index.html}. Every
 * other path, the REST API and the actuator included, is left alone.
 *
 * <p>The entry document is revalidated on every load: it is sent with {@code Cache-Control: no-cache} and without a
 * date validator. It names the hashed asset files of one build, and those names change with every build, so a browser
 * that keeps an old copy of it asks for assets that no longer exist and shows a blank page. The date validator is
 * left out because the packaged jar gives every file the same fixed date, so a conditional request would be answered
 * "not modified" after a rebuild as well. The hashed assets themselves keep the default static file handling.
 */
@Configuration(proxyBeanMethods = false)
public class StorefrontConfig implements WebMvcConfigurer {

    private static final String SHOP_INDEX = "forward:/shop/index.html";

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/shop/");
        registry.addViewController("/shop").setViewName(SHOP_INDEX);
        registry.addViewController("/shop/").setViewName(SHOP_INDEX);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // A literal pattern leaves the handler an empty resource path (404), so the file name is a capture variable
        // that only matches index.html.
        registry.addResourceHandler("/shop/{entry:index\\.html}")
                .addResourceLocations("classpath:/static/shop/")
                .setCacheControl(CacheControl.noCache())
                .setUseLastModified(false);
    }
}
