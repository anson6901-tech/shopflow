package com.shopflow.orders;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * The Orders REST API.
 * POST /orders       -> place an order
 * GET  /orders/{id}  -> look one up
 *
 * Placing an order ties the whole system together: it calls Catalog,
 * saves to the database, and publishes an event to SQS.
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository repository;
    private final CatalogClient catalogClient;
    private final OrderEventPublisher publisher;

    public OrderController(OrderRepository repository,
                           CatalogClient catalogClient,
                           OrderEventPublisher publisher) {
        this.repository = repository;
        this.catalogClient = catalogClient;
        this.publisher = publisher;
    }

    @PostMapping
    public ResponseEntity<Order> place(@RequestBody OrderRequest request) {
        // 1. Synchronous call to the Catalog service to check availability.
        if (!catalogClient.productIsAvailable(request.productId(), request.quantity())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Product is unavailable or out of stock");
        }
        // 2. Save the order to the database.
        Order saved = repository.save(new Order(request.productId(), request.quantity()));
        // 3. Asynchronously tell the rest of the system the order was placed.
        publisher.publishOrderPlaced(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> byId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** The JSON body of a place-order request. */
    public record OrderRequest(Long productId, int quantity) {
    }
}
