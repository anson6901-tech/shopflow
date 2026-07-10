package com.shopflow.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Orders service. It places orders: it checks the product with the
 * Catalog service, saves the order in the database, and publishes an
 * "order placed" event to SQS for the Notifications worker to pick up.
 */
@SpringBootApplication
public class OrdersApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrdersApplication.class, args);
    }
}
