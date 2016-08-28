package org.jdw.blog.common.messaging.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitKeyProperties {

    @Value("${rabbitmq.key.payload.find.id}")
    private String payloadFindId;

    public String getPayloadFindId() {
        return payloadFindId;
    }

}
