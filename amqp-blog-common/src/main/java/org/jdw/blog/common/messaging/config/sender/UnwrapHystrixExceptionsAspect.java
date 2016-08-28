package org.jdw.blog.common.messaging.config.sender;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.exception.HystrixRuntimeException;

/**
 * Negative Order value ensures this aspect executes before<br>
 * {@link HystrixCommand}, so this can unwrap the exceptions that throws.
 */
@Order(value = -1)
@Aspect
@Component
public class UnwrapHystrixExceptionsAspect {

    @Around("within(org.jdw..*) && @annotation(com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand)")
    public Object aroundMessageReceipt(final ProceedingJoinPoint joinPoint) throws Throwable {

        try {
            return joinPoint.proceed();
        } catch (HystrixRuntimeException e) {
            // Unwrap HystrixRuntimeExceptions if possible
            // to utilize the internal exception's @ResponseStatus
            Throwable cause = e.getCause();
            if (cause != null && cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw e;
        }
    }

}
