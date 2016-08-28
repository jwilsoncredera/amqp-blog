package org.jdw.blog.common.messaging.exception;

public abstract class RabbitException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RabbitException() {
    }

    public RabbitException(String message) {
        super(message);
    }

    public RabbitException(String message, Throwable e) {
        super(message, e);
    }

}
