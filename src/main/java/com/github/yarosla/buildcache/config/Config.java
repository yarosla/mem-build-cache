package com.github.yarosla.buildcache.config;

import com.github.yarosla.buildcache.Main;
import com.github.yarosla.buildcache.Parameters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class Config {
    private static final String APP_PROPERTIES_FILE = "app.properties";

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();
        pspc.setLocations(new ClassPathResource(APP_PROPERTIES_FILE));
        return pspc;
    }

    @Bean
    public Parameters parameters() {
        return Main.getParameters();
    }

    @Bean
    public long memoryLimit(Parameters parameters) {
        return parameters.getMemoryLimit();
    }
}
