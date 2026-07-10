package com.shopflow.notifications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Notifications worker. It does not handle web traffic from users.
 * Instead it watches the SQS queue and reacts to "order placed" events
 * by sending a confirmation. This is the asynchronous half of the system.
 */
@SpringBootApplication
public class NotificationsApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationsApplication.class, args);
    }
}
