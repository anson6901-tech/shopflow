package com.shopflow.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Extending JpaRepository gives us findAll, findById, save, and more,
 * with no implementation to write. This is the data layer.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
}
