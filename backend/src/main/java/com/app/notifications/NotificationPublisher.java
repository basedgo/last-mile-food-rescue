package com.app.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;


@Component
public class NotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationPublisher.class);

    private final String topicArn;
    private SnsClient snsClient;

    public NotificationPublisher(@Value("${app.notifications.topic-arn:}") String topicArn) {
        this.topicArn = topicArn;
    }


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotification(NotificationEvent event) {
        if (topicArn == null || topicArn.isBlank()) {
            log.info("[notification] {} | {}", event.subject(), event.message());
            return;
        }

        try {
            sns().publish(PublishRequest.builder()
                    .topicArn(topicArn)
                    .subject(event.subject())
                    .message(event.message())
                    .messageAttributes(Map.of("eventType", MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue(event.eventType())
                            .build()))
                    .build());
        } catch (RuntimeException exception) {
            // A failed notification must never break the claim/delivery itself.
            log.warn("Failed to publish {} notification to SNS", event.eventType(), exception);
        }
    }

    private synchronized SnsClient sns() {
        if (snsClient == null) {
            snsClient = SnsClient.builder()
                    .httpClient(UrlConnectionHttpClient.create())
                    .build();
        }
        return snsClient;
    }
}
