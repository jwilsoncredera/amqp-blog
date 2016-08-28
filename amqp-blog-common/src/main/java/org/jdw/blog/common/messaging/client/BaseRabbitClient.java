package org.jdw.blog.common.messaging.client;

import org.jdw.blog.common.messaging.config.RabbitKeyProperties;
import org.jdw.blog.common.messaging.exception.RabbitTimeoutException;
import org.jdw.blog.common.messaging.exception.RabbitUnexpectedTypeException;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseRabbitClient {

    @Autowired
    protected RabbitKeyProperties rabbitKeyProperties;

    @SuppressWarnings("unchecked")
    protected <T> T handleResponse(Object response, Class<T> responseClass) {

        if (response != null) {
            if (responseClass.isInstance(response)) {
                // Convert response to expected type
                return (T) response;

            } else if (response instanceof RuntimeException) {
                // Throw any Java exceptions returned instead of the expected type
                throw (RuntimeException) response;

            } else {
                throw new RabbitUnexpectedTypeException(responseClass, response.getClass());
            }
        }

        // RabbitTemplate returns null when timeouts occur.
        // If rabbitmq.connectionTimeout is shorter than the Hystrix timeout,
        // this could be reached.
        throw new RabbitTimeoutException(responseClass);
    }

}
