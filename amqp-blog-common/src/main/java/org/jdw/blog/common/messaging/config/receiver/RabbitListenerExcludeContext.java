package org.jdw.blog.common.messaging.config.receiver;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.core.context.SecurityContext;

/**
 * Used to avoid setting the {@link SecurityContext}<br>
 * in the thread receiving the AMQP message,<br>
 * via {@link RabbitSecurityContextAspect}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RabbitListenerExcludeContext {

}
