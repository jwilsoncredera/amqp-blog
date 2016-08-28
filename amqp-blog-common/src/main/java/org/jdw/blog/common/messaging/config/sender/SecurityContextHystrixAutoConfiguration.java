package org.jdw.blog.common.messaging.config.sender;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;

/**
 * Registers a custom Spring Security {@link HystrixConcurrencyStrategy} <br>
 * to ensure the security context is inherited into Hystrix threadpools.
 */
@Configuration
@ConditionalOnClass(HystrixCommand.class)
@ConditionalOnProperty(value = "forecasting.security.context.hystrix.strategy.enabled", matchIfMissing = true)
public class SecurityContextHystrixAutoConfiguration {

    @Bean
    SecurityContextHystrixConcurrencyStrategy securityContextHystrixConcurrencyStrategy() {
        return new SecurityContextHystrixConcurrencyStrategy();
    }

}
