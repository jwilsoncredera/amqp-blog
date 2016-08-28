package org.jdw.blog.common.messaging.config.receiver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * Negative Order value ensures this aspect executes before<br>
 * {@link Secured}, {@link PreAuthorize} or {@link RolesAllowed}<br>
 * which rely on the {@link SecurityContext} this initializes.
 */
@Order(value = -1)
@Aspect
@Component
public class RabbitSecurityContextAspect extends BaseRabbitContextAspect {

    public final static String AUTH_USERNAME_HEADER = "auth_username";
    public final static String AUTH_USERID_HEADER = "auth_userid";
    public final static String AUTH_ROLES_HEADER_COMMA_DELIMITED = "auth_roles_comma_delimited";

    @Around("within(org.jdw..*) && @annotation(org.springframework.amqp.rabbit.annotation.RabbitListener) && !@annotation(RabbitListenerExcludeContext)")
    public Object aroundMessageReceipt(final ProceedingJoinPoint joinPoint) throws Throwable {

        Map<String, Object> headers = extractHeaders(joinPoint);
        try {
            setSecurityContext(headers);
            return joinPoint.proceed();
        } catch (RuntimeException e) {
            // Return exceptions to the caller for error propagation.
            if (!ClassUtils.hasConstructor(e.getClass())) {
                // The returned value must have a no-arg constructor
                // to be deserializable by Jackson.
                return new RuntimeException(e);
            } else {
                return e;
            }
        } finally {
            clearSecurityContext();
        }
    }

    protected void setSecurityContext(Map<String, Object> headers) {

        List<GrantedAuthority> roles;
        Object rolesHeaderValue = headers.get(AUTH_ROLES_HEADER_COMMA_DELIMITED);
        if (rolesHeaderValue != null) {
            roles = Arrays.asList(StringUtils.split(rolesHeaderValue.toString(), ",")).stream()
                    .filter(r -> StringUtils.isNotBlank(r))
                    .map(r -> new SimpleGrantedAuthority(r.trim()))
                    .collect(Collectors.toList());
        } else {
            roles = new ArrayList<>(0);
        }

        String username = null;
        Object usernameHeaderValue = headers.get(AUTH_USERNAME_HEADER);
        if (usernameHeaderValue != null) {
            username = usernameHeaderValue.toString();
        }

        // The password doesn't need to be valid,
        // we're assuming the user is authenticated if their request already invoked an AMQP call
        Authentication authentication = new UsernamePasswordAuthenticationToken(username, "fake_password", roles);

        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);
    }

    protected void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

}
