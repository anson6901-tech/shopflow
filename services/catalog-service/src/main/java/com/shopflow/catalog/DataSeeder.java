package com.shopflow.catalog;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Puts a few products in the database on startup so the store is not
 * empty the first time you open it. Runs only if there are no products yet.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository repository;

    public DataSeeder(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        repository.saveAll(List.of(
                new Product("Wireless Mouse", 24.99, 120),
                new Product("Mechanical Keyboard", 79.50, 60),
                new Product("USB-C Hub", 39.00, 200),
                new Product("Laptop Stand", 32.25, 85)
        ));
    }
}
