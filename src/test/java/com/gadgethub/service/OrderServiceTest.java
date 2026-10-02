package com.gadgethub.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.gadgethub.dto.OrderDtos.ItemRequest;
import com.gadgethub.dto.OrderDtos.OrderRequest;
import com.gadgethub.dto.OrderDtos.OrderResponse;
import com.gadgethub.exception.BusinessRuleException;
import com.gadgethub.model.*;
import com.gadgethub.repository.CustomerRepository;
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
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ProductRepository productRepository;
    @InjectMocks private OrderService orderService;

    private Customer customer() {
        Customer c = new Customer();
        c.setId(1L);
        c.setName("Ada");
        c.setEmail("ada@test.com");
        return c;
    }

    private Product product(int stock) {
        Product p = new Product();
        p.setId(5L);
        p.setName("Smart Watch");
        p.setPrice(new BigDecimal("20.00"));
        p.setStock(stock);
        return p;
    }

    @Test
    void placeOrder_calculatesTotal_andReducesStock() {
        Product p = product(10);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer()));
        when(productRepository.findById(5L)).thenReturn(Optional.of(p));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse res = orderService.placeOrder(new OrderRequest(1L, List.of(new ItemRequest(5L, 3))));

        assertEquals(0, new BigDecimal("60.00").compareTo(res.total()));
        assertEquals(7, p.getStock().intValue());
        assertEquals(OrderStatus.PLACED, res.status());
    }

    @Test
    void placeOrder_insufficientStock_throwsAndSavesNothing() {
        Product p = product(2);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer()));
        when(productRepository.findById(5L)).thenReturn(Optional.of(p));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderService.placeOrder(new OrderRequest(1L, List.of(new ItemRequest(5L, 5)))));

        assertTrue(ex.getMessage().contains("Insufficient stock"));
        assertEquals(2, p.getStock().intValue());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancel_restoresStock_andMarksCancelled() {
        Product p = product(7);
        Order order = new Order();
        order.setCustomer(customer());
        order.getItems().add(new OrderItem(order, p, 3, p.getPrice()));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse res = orderService.cancel(1L);

        assertEquals(10, p.getStock().intValue());
        assertEquals(OrderStatus.CANCELLED, res.status());
    }

    @Test
    void cancel_whenAlreadyCancelled_throws() {
        Order order = new Order();
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(BusinessRuleException.class, () -> orderService.cancel(1L));
    }
}
