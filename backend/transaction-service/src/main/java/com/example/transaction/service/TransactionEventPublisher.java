package com.example.transaction.service;

import com.example.transaction.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${example.transaction.events.transport:rabbitmq}")
    private String eventTransport;

    @Value("${example.transaction.sqs.queue-url:}")
    private String sqsQueueUrl;

    @Value("${example.transaction.rabbitmq.exchanges.transaction}")
    private String transactionExchange;

    @Value("${example.transaction.rabbitmq.routing-keys.transaction-created}")
    private String transactionCreatedRoutingKey;

    public void publishTransactionCompleted(Transaction transaction) {

        Map<String, Object> event = new HashMap<>();

        event.put("eventType", "TRANSACTION_COMPLETED");
        event.put("transactionId", transaction.getId());
        event.put("sourceAccount", transaction.getSourceAccount());
        event.put("targetAccount", transaction.getTargetAccount());
        event.put("amount", transaction.getAmount());
        event.put("currency", transaction.getCurrency());
        event.put("reference", transaction.getReference());
        event.put("initiatedBy", transaction.getInitiatedBy());

        try {
            if ("sqs".equalsIgnoreCase(eventTransport)) {
                if (sqsQueueUrl.isBlank()) {
                    throw new IllegalStateException("SQS Queue URL is missing");
                }

                Map<String, String> sqsEvent = Map.of(
                    "eventType", "TRANSACTION_COMPLETED",
                    "transactionId", String.valueOf(transaction.getId())
                );

                try (SqsClient sqs = SqsClient.create()) {
                    sqs.sendMessage(
                        SendMessageRequest.builder()
                            .queueUrl(sqsQueueUrl)
                            .messageBody(objectMapper.writeValueAsString(sqsEvent))
                            .build()
                    );
                }
            } else {
                rabbitTemplate.convertAndSend(
                    transactionExchange,
                    transactionCreatedRoutingKey,
                    event
                );
            }

            log.info(
                    "Published TRANSACTION_COMPLETED event: {}",
                    transaction.getReference()
            );

        } catch (Exception e) {
            log.error(
                    "Failed to publish TRANSACTION_COMPLETED event: {}",
                    e.getMessage()
            );
        }
    }
}