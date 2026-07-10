package com.shopflow.orders;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Sends an "order placed" message to the SQS queue. This is asynchronous
 * communication: we drop the message and move on. We do not wait for the
 * Notifications worker, so a slow confirmation never slows down checkout.
 */
@Component
public class OrderEventPublisher {

    private final SqsTemplate sqsTemplate;
    private final String queueName;

    public OrderEventPublisher(SqsTemplate sqsTemplate,
                               @Value("${order-events.queue-name}") String queueName) {
        this.sqsTemplate = sqsTemplate;
        this.queueName = queueName;
    }

    public void publishOrderPlaced(Order order) {
        String message = "{\"orderId\":" + order.getId()
                + ",\"productId\":" + order.getProductId()
                + ",\"quantity\":" + order.getQuantity() + "}";
        sqsTemplate.send(queueName, message);
    }
}
