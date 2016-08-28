package org.jdw.blog.common.messaging.config.sender;

import org.jdw.blog.common.messaging.config.receiver.RabbitSleuthContextAspect;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.hystrix.SleuthHystrixConcurrencyStrategy;
import org.springframework.stereotype.Component;

/**
 * Based on {@link SleuthHystrixConcurrencyStrategy}
 */
@Component("rabbitSleuthForwarder")
public class RabbitSleuthForwarder implements MessagePostProcessor {

    @Autowired
    private Tracer tracer;

    @Override
    public Message postProcessMessage(Message message) throws AmqpException {

        MessageProperties messageProperties = message.getMessageProperties();

        // The span should always exist.
        // If it doesn't, it's too late to create one here since it would need to be closed immediately after the message is processed
        // to guarantee threadlocal cleanup, which would prevent it from being used in further logs in this service, removing its usefulness.
        Span span = tracer.getCurrentSpan();
        if (span != null) {
            long traceId = span.getTraceId();
            if (traceId != 0L && traceId != -1L) {
                // Ensure the trace ID is maintained across AMQP calls by passing it in a header
                messageProperties.setHeader(RabbitSleuthContextAspect.SLEUTH_TRACE_ID_HEADER, Long.toString(traceId));
            }
        }

        return message;
    }

}
