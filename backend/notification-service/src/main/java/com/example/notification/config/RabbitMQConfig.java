package com.example.notification.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${example.notification.rabbitmq.queues.account-created}")
    private String accountCreatedQueue;

    @Value("${example.notification.rabbitmq.queues.transaction-created}")
    private String transactionCreatedQueue;

    @Bean
    public Queue accountCreatedQueue() {
        return QueueBuilder
                .durable(accountCreatedQueue)
                .build();
    }

    @Bean
    public Queue transactionCreatedQueue() {
        return QueueBuilder
                .durable(transactionCreatedQueue)
                .build();
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory) {

        RabbitTemplate template =
                new RabbitTemplate(connectionFactory);

        template.setMessageConverter(messageConverter());

        return template;
    }
}