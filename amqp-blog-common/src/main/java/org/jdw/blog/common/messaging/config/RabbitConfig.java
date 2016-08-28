package org.jdw.blog.common.messaging.config;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rabbitmq.client.ConnectionFactory;

@Configuration
@EnableRabbit
public class RabbitConfig {

    @Autowired
    private RabbitKeyProperties rabbitKeyProperties;

    @Value("${rabbitmq.virtualhost:#{null}}")
    private String rabbitMqVirtualHost;

    @Value("${rabbitmq.username:#{null}}")
    private String rabbitMqUsername;

    @Value("${rabbitmq.password:#{null}}")
    private String rabbitMqPassword;

    @Value("${rabbitmq.hosts:#{null}}")
    private String rabbitMqHosts;

    @Value("${rabbitmq.port:#{null}}")
    private Integer rabbitMqPort;

    @Value("${rabbitmq.uri:#{null}}")
    private String rabbitMqUri;

    @Value("${rabbitmq.heartbeat}")
    private Integer rabbitHeartbeat;

    @Value("${rabbitmq.connectionTimeout}")
    private Integer rabbitmqConnectionTimeout;

    @Value("${rabbitmq.autoStartup}")
    private boolean rabbitAutoStartup;

    @Value("${rabbitmq.concurrentConsumers.min}")
    private int minConcurrentConsumers;

    @Value("${rabbitmq.concurrentConsumers.max}")
    private int maxConcurrentConsumers;

    @Value("${rabbitmq.exchange.payload}")
    private String payloadExchange;

    @Autowired
    @Qualifier("rabbitAuthenticationForwarder")
    private MessagePostProcessor rabbitAuthenticationForwarder;

    @Autowired
    @Qualifier("rabbitSleuthForwarder")
    private MessagePostProcessor rabbitSleuthForwarder;

    @Bean
    public ConnectionFactory rabbitConnectionFactory() {

        ConnectionFactory connectionFactory = new ConnectionFactory();

        if (rabbitMqVirtualHost != null) {
            connectionFactory.setVirtualHost(rabbitMqVirtualHost);
        }

        if (rabbitMqPort != null) {
            connectionFactory.setPort(rabbitMqPort);

            if (rabbitMqPort != 5672 && rabbitMqPort != 5671) {
                try {
                    connectionFactory.useSslProtocol();
                } catch (KeyManagementException | NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        connectionFactory.setUsername(rabbitMqUsername);
        connectionFactory.setPassword(rabbitMqPassword);
        connectionFactory.setRequestedHeartbeat(rabbitHeartbeat);
        connectionFactory.setConnectionTimeout(rabbitmqConnectionTimeout);

        return connectionFactory;
    }

    @Bean
    public CachingConnectionFactory rabbitCachingConnectionFactory(ConnectionFactory connectionFactory) {
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(connectionFactory);
        if (rabbitMqHosts != null) {
            cachingConnectionFactory.setAddresses(rabbitMqHosts);
        } else if (rabbitMqUri != null) {
            cachingConnectionFactory.setUri(rabbitMqUri);
        }

        return cachingConnectionFactory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(CachingConnectionFactory cachingConnectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(cachingConnectionFactory);
        rabbitAdmin.afterPropertiesSet();
        return rabbitAdmin;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            CachingConnectionFactory cachingConnectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(cachingConnectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAutoStartup(rabbitAutoStartup);
        factory.setConcurrentConsumers(minConcurrentConsumers);
        factory.setMaxConcurrentConsumers(maxConcurrentConsumers);
        // If a listener throws a runtime exception,
        // do not put the message back on the queue
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    protected RabbitTemplate createRabbitTemplate(CachingConnectionFactory cachingConnectionFactory, String exchange) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(cachingConnectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        rabbitTemplate.setExchange(exchange);
        rabbitTemplate.setBeforePublishPostProcessors(rabbitAuthenticationForwarder, rabbitSleuthForwarder);
        rabbitTemplate.setReplyTimeout(rabbitmqConnectionTimeout);
        return rabbitTemplate;
    }

    @Bean
    public RabbitTemplate payloadRabbitTemplate(CachingConnectionFactory cachingConnectionFactory) {
        return createRabbitTemplate(cachingConnectionFactory, payloadExchange);
    }

    @Bean
    public DirectExchange payloadExchange() {
        return new DirectExchange(payloadExchange, true, false);
    }

    @Bean
    public Queue payloadFindIdQueue() {
        return new Queue(rabbitKeyProperties.getPayloadFindId(), true);
    }

    @Bean
    public Binding payloadFindIdRequestQueueBinding(@Qualifier("payloadExchange") Exchange exchange,
            @Qualifier("payloadFindIdQueue") Queue queue) {
        return BindingBuilder.bind(queue).to(exchange).with(rabbitKeyProperties.getPayloadFindId()).noargs();
    }

}
