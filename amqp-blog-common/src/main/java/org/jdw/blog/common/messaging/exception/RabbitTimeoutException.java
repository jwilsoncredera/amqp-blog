package org.jdw.blog.common.messaging.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class RabbitTimeoutException extends RabbitException {

    private static final long serialVersionUID = 1L;

    public RabbitTimeoutException() {
    }

    public RabbitTimeoutException(Class<?> expected) {
        super("Timeout occurred, expected " + expected.getSimpleName());
    }

}
