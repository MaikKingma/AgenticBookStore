package nl.openvalue.distributor.api;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the {@link ChaosInterceptor} on {@code /api/**} only. The properties are enabled here, on a bean that the
 * web slice tests load, because {@code @ConfigurationPropertiesScan} is not honoured in a {@code @WebMvcTest}.
 *
 * <p>The one interceptor serves every request thread at once, and the JDK's default generator is not thread-safe. So
 * every draw asks the calling thread for its own generator ({@code ThreadLocalRandom::current}); no generator is
 * created here and shared.
 */
@Component
@EnableConfigurationProperties(ChaosProperties.class)
public class ChaosWebConfig implements WebMvcConfigurer {

    private final ChaosProperties properties;

    public ChaosWebConfig(ChaosProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(
                        new ChaosInterceptor(properties, ThreadLocalRandom::current, ChaosInterceptor::sleepMillis))
                .addPathPatterns("/api/**");
    }
}
