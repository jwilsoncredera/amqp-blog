package org.jdw.blog.common.messaging.config.receiver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.messaging.handler.annotation.Headers;

public abstract class BaseRabbitContextAspect {

    /**
     * Extracts the RabbitMQ headers from the joinPoint.
     * <p>
     * The headers might be {@code null} if the wrapped method<br>
     * did not expose them with the {@link Headers} annotation.
     */
    protected Map<String, Object> extractHeaders(ProceedingJoinPoint joinPoint) {
        Map<String, Object> headers = null;

        Object[] args = joinPoint.getArgs();
        if (args != null) {
            // Find the @Headers argument
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();

            argsLoop: for (int i = 0; i < args.length && i < parameterAnnotations.length; i++) {
                Annotation[] argAnnotations = parameterAnnotations[i];
                if (argAnnotations != null) {

                    // There could be multiple annotations on the argument
                    for (Annotation argAnnotation : argAnnotations) {
                        Class<?> argAnnotationType = argAnnotation.annotationType();

                        if (headers == null && argAnnotationType.equals(Headers.class)) {
                            headers = typecastHeaders(args[i]);
                            break argsLoop;
                        }
                    }
                }
            }
        }

        return headers;
    }

    /**
     * {@link Headers} can only be used on an argument of type<br>
     * Map<String, Object> so the typecast is safe.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> typecastHeaders(Object object) {
        return (Map<String, Object>) object;
    }
}
