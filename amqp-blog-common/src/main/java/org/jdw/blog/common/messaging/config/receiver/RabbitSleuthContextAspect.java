package org.jdw.blog.common.messaging.config.receiver;

import java.util.Map;
import java.util.Random;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.hystrix.SleuthHystrixConcurrencyStrategy;
import org.springframework.stereotype.Component;

/**
 * Ensures Sleuth logging correlation IDs continue from the caller.
 * <p>
 * Based on {@link SleuthHystrixConcurrencyStrategy}
 */
@Aspect
@Component
public class RabbitSleuthContextAspect extends BaseRabbitContextAspect {

    @Autowired
    private Tracer tracer;

    @Autowired
    private Random random;

    public final static String SLEUTH_TRACE_ID_HEADER = "sleuth_trace_id";

    private final static String RABBITMQ_SPAN_COMPONENT = "rabbitmq";

    @Around("within(org.jdw..*) && @annotation(org.springframework.amqp.rabbit.annotation.RabbitListener) && !@annotation(RabbitListenerExcludeContext)")
    public Object aroundMessageReceipt(final ProceedingJoinPoint joinPoint) throws Throwable {

        Map<String, Object> headers = extractHeaders(joinPoint);
        Span sleuthSpan = null;
        try {
            sleuthSpan = setSleuthContext(headers);
            return joinPoint.proceed();
        } finally {
            clearSleuthContext(sleuthSpan);
        }
    }

    protected Span setSleuthContext(Map<String, Object> headers) {
        Span span = null;

        Object sleuthHeaderValue = headers.get(SLEUTH_TRACE_ID_HEADER);
        if (sleuthHeaderValue != null) {
            String sleuthHeader = sleuthHeaderValue.toString();
            Long traceId = Long.parseLong(sleuthHeader);

            // Start new sleuth span, retaining the previous trace ID
            long spanId = this.random.nextLong();
            span = Span.builder().name(RABBITMQ_SPAN_COMPONENT).traceId(traceId)
                    .spanId(spanId).build();
            tracer.continueSpan(span);
        }

        return span;
    }

    protected void clearSleuthContext(Span span) {
        if (span != null) {
            tracer.close(span);
        }
    }

}
