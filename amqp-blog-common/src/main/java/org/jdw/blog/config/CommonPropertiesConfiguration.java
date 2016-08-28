package org.jdw.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CommonPropertiesConfiguration extends PropertiesConfiguration {

    @Bean
    @Profile(Profiles.NOT_TEST)
    public static PropertySourcesPlaceholderConfigurer commonProperties() {
        return createPropertySourcesPlaceholderConfigurer(
                "common_application.properties");
    }

    @Bean
    @Profile(Profiles.TEST)
    public static PropertySourcesPlaceholderConfigurer testProperties() {
        return createPropertySourcesPlaceholderConfigurer(
                "common_application.properties",
                "common_test_application.properties");
    }

}
