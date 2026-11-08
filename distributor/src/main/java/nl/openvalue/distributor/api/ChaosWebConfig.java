package nl.openvalue.distributor.api;

import java.util.random.RandomGenerator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the {@link ChaosInterceptor} on {@code /api/**} only. The properties are enabled here, on a bean that the
 * web slice tests load, because {@code @ConfigurationPropertiesScan} is not honoured in a {@code @WebMvcTest}.
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
                        new ChaosInterceptor(properties, RandomGenerator.getDefault(), ChaosInterceptor::sleepMillis))
                .addPathPatterns("/api/**");
    }
}
