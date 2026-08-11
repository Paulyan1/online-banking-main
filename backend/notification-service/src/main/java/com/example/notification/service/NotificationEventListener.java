package com.example.notification.service;

import com.example.notification.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    @RabbitListener(
            queues = "${example.notification.rabbitmq.queues.account-created}"
    )
    public void onAccountCreated(Map<String, Object> event) {

        log.info("Received ACCOUNT_CREATED event: {}", event);

        String userId = String.valueOf(event.get("userId"));
        String accountNumber = String.valueOf(event.get("accountNumber"));
        String ownerName = String.valueOf(event.get("ownerName"));
        String accountId = String.valueOf(event.get("accountId"));

        notificationService.createAndSend(
                userId,
                Notification.NotificationType.ACCOUNT_CREATED,
                "Account Created",
                "Your account " + accountNumber
                        + " has been successfully created, "
                        + ownerName + "!",
                accountId
        );
    }

    @RabbitListener(
            queues = "${example.notification.rabbitmq.queues.transaction-created}"
    )
    public void onTransactionCompleted(Map<String, Object> event) {

        log.info("Received TRANSACTION_COMPLETED event: {}", event);

        String userId = String.valueOf(event.get("initiatedBy"));
        String amount = String.valueOf(event.get("amount"));
        String currency = String.valueOf(event.get("currency"));
        String targetAccount = String.valueOf(event.get("targetAccount"));
        String reference = String.valueOf(event.get("reference"));

        notificationService.createAndSend(
                userId,
                Notification.NotificationType.TRANSACTION_COMPLETED,
                "Transfer Completed",
                "Transfer of " + amount
                        + " " + currency
                        + " to " + targetAccount
                        + " completed. Ref: " + reference,
                reference
        );
    }
}