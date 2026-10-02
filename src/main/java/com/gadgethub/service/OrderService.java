package com.gadgethub.service;

import com.gadgethub.dto.OrderDtos.ItemRequest;
import com.gadgethub.dto.OrderDtos.OrderRequest;
import com.gadgethub.dto.OrderDtos.OrderResponse;
import com.gadgethub.exception.BusinessRuleException;
import com.gadgethub.exception.ResourceNotFoundException;
import com.gadgethub.model.Customer;
import com.gadgethub.model.Order;
import com.gadgethub.model.OrderItem;
import com.gadgethub.model.OrderStatus;
import com.gadgethub.model.Product;
import com.gadgethub.repository.CustomerRepository;
import com.gadgethub.repository.OrderRepository;
import com.gadgethub.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    /** One transaction: if ANY item fails, stock changes and the order are all rolled back. */
    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer " + request.customerId() + " not found"));

        Order order = new Order();
        order.setCustomer(customer);
        BigDecimal total = BigDecimal.ZERO;

        for (ItemRequest line : request.items()) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product " + line.productId() + " not found"));

            int qty = line.quantity();
            if (product.getStock() < qty) {
                throw new BusinessRuleException("Insufficient stock for '" + product.getName()
                        + "': requested " + qty + ", available " + product.getStock());
            }
            product.setStock(product.getStock() - qty);

            order.getItems().add(new OrderItem(order, product, qty, product.getPrice()));
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(qty)));
        }

        order.setTotal(total);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        return OrderResponse.from(findOrder(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list(Long customerId) {
        List<Order> orders = (customerId == null)
                ? orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                : orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        return orders.stream().map(OrderResponse::from).toList();
    }

    @Transactional
    public OrderResponse ship(Long id) {
        Order order = findOrder(id);
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new BusinessRuleException("Only PLACED orders can be shipped (current: " + order.getStatus() + ")");
        }
        order.setStatus(OrderStatus.SHIPPED);
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancel(Long id) {
        Order order = findOrder(id);
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new BusinessRuleException("Only PLACED orders can be cancelled (current: " + order.getStatus() + ")");
        }
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
        }
        order.setStatus(OrderStatus.CANCELLED);
        return OrderResponse.from(order);
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + id + " not found"));
    }
}
