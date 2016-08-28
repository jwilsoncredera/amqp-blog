package org.jdw.blog.common.messaging.config.sender;

import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.sleuth.instrument.hystrix.SleuthHystrixConcurrencyStrategy;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;

/**
 * Based on {@link SleuthHystrixConcurrencyStrategy}.
 * <p>
 * Ensures the Spring Security context is inherited into Hystrix threadpools<br>
 * by utilizing {@link HystrixConcurrencyStrategy#wrapCallable(Callable)}.
 */
public class SecurityContextHystrixConcurrencyStrategy extends HystrixConcurrencyStrategy {

    private static final Log log = LogFactory.getLog(SecurityContextHystrixConcurrencyStrategy.class);

    private HystrixConcurrencyStrategy delegate;

    public SecurityContextHystrixConcurrencyStrategy() {
        try {
            // Wrap any existing concurrency strategies (such as SleuthConcurrencyStrategy)
            this.delegate = HystrixPlugins.getInstance().getConcurrencyStrategy();
            if (this.delegate instanceof SecurityContextHystrixConcurrencyStrategy) {
                // Don't let this wrap itself
                return;
            }

            HystrixCommandExecutionHook commandExecutionHook = HystrixPlugins.getInstance().getCommandExecutionHook();
            HystrixEventNotifier eventNotifier = HystrixPlugins.getInstance().getEventNotifier();
            HystrixMetricsPublisher metricsPublisher = HystrixPlugins.getInstance().getMetricsPublisher();
            HystrixPropertiesStrategy propertiesStrategy = HystrixPlugins.getInstance().getPropertiesStrategy();
            logCurrentStateOfHysrixPlugins(eventNotifier, metricsPublisher, propertiesStrategy);

            HystrixPlugins.reset();
            HystrixPlugins.getInstance().registerConcurrencyStrategy(this);
            HystrixPlugins.getInstance().registerCommandExecutionHook(commandExecutionHook);
            HystrixPlugins.getInstance().registerEventNotifier(eventNotifier);
            HystrixPlugins.getInstance().registerMetricsPublisher(metricsPublisher);
            HystrixPlugins.getInstance().registerPropertiesStrategy(propertiesStrategy);
        } catch (Exception e) {
            log.error("Failed to register Spring Security Hystrix Concurrency Strategy", e);
        }
    }

    private void logCurrentStateOfHysrixPlugins(HystrixEventNotifier eventNotifier,
            HystrixMetricsPublisher metricsPublisher,
            HystrixPropertiesStrategy propertiesStrategy) {
        log.debug("Current Hystrix plugins configuration is [" + "concurrencyStrategy ["
                + this.delegate + "]," + "eventNotifier [" + eventNotifier + "],"
                + "metricPublisher [" + metricsPublisher + "]," + "propertiesStrategy ["
                + propertiesStrategy + "]," + "]");
        log.debug("Registering Spring Security Hystrix Concurrency Strategy.");
    }

    @Override
    public <T> Callable<T> wrapCallable(Callable<T> callable) {
        if (callable instanceof HystrixSecurityContextCallable) {
            // Don't let this wrap itself
            return callable;
        }

        Callable<T> wrappedCallable = this.delegate != null
                ? this.delegate.wrapCallable(callable) : callable;
        if (wrappedCallable instanceof HystrixSecurityContextCallable) {
            // Don't let this wrap itself
            return wrappedCallable;
        }

        // Create a callable that will eventually contain the Hystrix command callable.
        // The immediate callable it wraps might be another callable that wraps the Hystrix command,
        // of this callable might even be wrapped by other callables itself, all enhancing the Hystrix thread.
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return new HystrixSecurityContextCallable<>(securityContext, wrappedCallable);
    }

    // Visible for testing
    static class HystrixSecurityContextCallable<S> implements Callable<S> {

        private SecurityContext securityContext;
        private Callable<S> callable;

        public HystrixSecurityContextCallable(SecurityContext securityContext, Callable<S> callable) {
            this.securityContext = securityContext;
            this.callable = callable;
        }

        @Override
        public S call() throws Exception {
            try {
                SecurityContextHolder.setContext(securityContext);
                return this.callable.call();
            } finally {
                SecurityContextHolder.clearContext();
            }
        }

    }
}
