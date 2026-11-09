package nl.openvalue.bookstore;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Entry points of the storefront, a single page application served from {@code static/shop}: the root of the site
 * redirects to {@code /shop/}, and both {@code /shop} and {@code /shop/} show the page's {@code index.html}. Every
 * other path, the REST API and the actuator included, is left alone.
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
}
