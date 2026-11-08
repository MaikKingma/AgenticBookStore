package nl.openvalue.distributor.catalog;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
public class CatalogConfig {

    /**
     * Every date calculation (delivery dates, {@code releasedDaysAgo}) uses this clock, so "today" is the
     * workshop's wall-clock day in the Netherlands whatever zone the laptop is set to.
     */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Europe/Amsterdam"));
    }

    @Bean
    public TitleCatalog titleCatalog(JsonMapper jsonMapper, Clock clock) {
        return new CatalogLoader(jsonMapper, clock).loadSeed();
    }
}
