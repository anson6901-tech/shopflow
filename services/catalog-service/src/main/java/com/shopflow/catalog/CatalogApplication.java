package com.shopflow.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Catalog service. It owns products and inventory and exposes them
 * over a small REST API that the Storefront and the Orders service call.
 */
@SpringBootApplication
public class CatalogApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatalogApplication.class, args);
    }
}
