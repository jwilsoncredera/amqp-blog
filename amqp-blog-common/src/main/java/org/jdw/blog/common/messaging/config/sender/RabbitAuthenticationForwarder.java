package org.jdw.blog.common.messaging.config.sender;

import java.util.StringJoiner;

import org.jdw.blog.common.messaging.config.receiver.RabbitSecurityContextAspect;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("rabbitAuthenticationForwarder")
public class RabbitAuthenticationForwarder implements MessagePostProcessor {

    @Override
    public Message postProcessMessage(Message message) throws AmqpException {

        MessageProperties messageProperties = message.getMessageProperties();

        // Forward security context information to recipient,
        // where RabbitSecurityContextAspect will reconstruct the context
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext != null) {
            Authentication authentication = securityContext.getAuthentication();
            if (authentication != null) {

                // Roles may be available even without a principal
                StringJoiner commaDelimitedRoles = new StringJoiner(",");
                for (GrantedAuthority authority : authentication.getAuthorities()) {
                    commaDelimitedRoles.add(authority.getAuthority());
                }
                messageProperties.setHeader(RabbitSecurityContextAspect.AUTH_ROLES_HEADER_COMMA_DELIMITED,
                        commaDelimitedRoles.toString());

                Object principal = authentication.getPrincipal();
                if (principal != null) {
                    String username = principal.toString();
                    messageProperties.setHeader(RabbitSecurityContextAspect.AUTH_USERNAME_HEADER, username);
                }
            }
        }

        return message;
    }

}
