package com.shopflow.notifications;

import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Listens to the order-events queue. Every time the Orders service publishes
 * an "order placed" message, this method runs with that message. In a real
 * system it would send an email or text; here it logs a confirmation, which
 * is enough to prove the asynchronous flow works end to end.
 */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @SqsListener("${order-events.queue-name}")
    public void onOrderPlaced(String message) {
        log.info("Confirmation sent for order event: {}", message);
    }
}
