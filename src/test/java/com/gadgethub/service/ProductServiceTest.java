package com.gadgethub.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.gadgethub.exception.BusinessRuleException;
import com.gadgethub.exception.ResourceNotFoundException;
import com.gadgethub.model.Product;
import com.gadgethub.repository.OrderRepository;
import com.gadgethub.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @InjectMocks private ProductService productService;

    private Product product(String name) {
        Product p = new Product();
        p.setId(1L);
        p.setName(name);
        p.setCategory("Audio");
        p.setPrice(new BigDecimal("10.00"));
        p.setStock(5);
        return p;
    }

    @Test
    void findById_missing_throwsNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> productService.findById(1L));
    }

    @Test
    void findAll_withCategory_usesCategoryQuery() {
        when(productRepository.findByCategoryIgnoreCase("Audio")).thenReturn(List.of(product("Earbuds")));
        assertEquals(1, productService.findAll("Audio", null).size());
    }

    @Test
    void update_changesFields() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product("Old")));
        Product in = product("New");
        in.setPrice(new BigDecimal("99.00"));
        in.setStock(4);

        Product result = productService.update(1L, in);

        assertEquals("New", result.getName());
        assertEquals(0, new BigDecimal("99.00").compareTo(result.getPrice()));
        assertEquals(4, result.getStock().intValue());
    }

    @Test
    void delete_productInOrders_isBlocked() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product("Earbuds")));
        when(orderRepository.countItemsByProductId(1L)).thenReturn(2L);

        assertThrows(BusinessRuleException.class, () -> productService.delete(1L));
        verify(productRepository, never()).delete(any(Product.class));
    }
}
