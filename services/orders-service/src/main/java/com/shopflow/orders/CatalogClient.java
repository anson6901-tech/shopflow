package com.shopflow.orders;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Talks to the Catalog service. This is synchronous service-to-service
 * communication: the Orders service makes an HTTP call and waits for the answer.
 *
 * The address (CATALOG_URL) is the Catalog service's Kubernetes name, for
 * example http://catalog-service:8080. Kubernetes resolves that name to the
 * right pods, so we never hardcode an IP. This is service discovery by DNS.
 */
@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(@Value("${catalog.url}") String catalogUrl) {
        this.restClient = RestClient.create(catalogUrl);
    }

    /** Returns true if the product exists and has enough stock. */
    public boolean productIsAvailable(Long productId, int quantity) {
        try {
            Product product = restClient.get()
                    .uri("/products/{id}", productId)
                    .retrieve()
                    .body(Product.class);
            return product != null && product.stock() >= quantity;
        } catch (Exception e) {
            // If Catalog is down or the product is missing, treat it as unavailable.
            return false;
        }
    }

    /** A small shape that matches the JSON the Catalog service returns. */
    public record Product(Long id, String name, double price, int stock) {
    }
}
