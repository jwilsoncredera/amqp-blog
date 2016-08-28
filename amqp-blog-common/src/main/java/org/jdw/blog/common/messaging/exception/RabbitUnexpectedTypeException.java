package org.jdw.blog.common.messaging.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class RabbitUnexpectedTypeException extends RabbitException {

    private static final long serialVersionUID = 1L;

    public RabbitUnexpectedTypeException() {
    }

    public RabbitUnexpectedTypeException(Class<?> expected, Class<?> actual) {
        super("Expected " + expected.getSimpleName() + " but received " + actual.getSimpleName());
    }

}
