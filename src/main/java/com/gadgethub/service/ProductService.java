package com.gadgethub.service;

import com.gadgethub.exception.BusinessRuleException;
import com.gadgethub.exception.ResourceNotFoundException;
import com.gadgethub.model.Product;
import com.gadgethub.repository.OrderRepository;
import com.gadgethub.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public ProductService(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll(String category, String search) {
        if (category != null && !category.isBlank()) {
            return productRepository.findByCategoryIgnoreCase(category.trim());
        }
        if (search != null && !search.isBlank()) {
            return productRepository.findByNameContainingIgnoreCase(search.trim());
        }
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " not found"));
    }

    @Transactional
    public Product create(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, Product in) {
        Product p = findById(id);
        p.setName(in.getName());
        p.setCategory(in.getCategory());
        p.setBrand(in.getBrand());
        p.setPrice(in.getPrice());
        p.setStock(in.getStock());
        p.setDescription(in.getDescription());
        return p;
    }

    @Transactional
    public void delete(Long id) {
        Product p = findById(id);
        if (orderRepository.countItemsByProductId(id) > 0) {
            throw new BusinessRuleException("Cannot delete '" + p.getName() + "': it appears in existing orders");
        }
        productRepository.delete(p);
    }
}
