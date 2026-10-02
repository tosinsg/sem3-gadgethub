package com.gadgethub.dto;

import com.gadgethub.model.Order;
import com.gadgethub.model.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDtos {

    private OrderDtos() { }

    public record ItemRequest(
            @NotNull(message = "productId is required") Long productId,
            @NotNull(message = "quantity is required") @Min(value = 1, message = "quantity must be at least 1") Integer quantity) { }

    public record OrderRequest(
            @NotNull(message = "customerId is required") Long customerId,
            @NotEmpty(message = "An order needs at least one item") @Valid List<ItemRequest> items) { }

    public record ItemResponse(Long productId, String productName, int quantity, BigDecimal unitPrice, BigDecimal subtotal) { }

    public record OrderResponse(Long id, Long customerId, String customerName, List<ItemResponse> items,
                                BigDecimal total, OrderStatus status, LocalDateTime createdAt) {

        public static OrderResponse from(Order o) {
            List<ItemResponse> items = o.getItems().stream()
                    .map(i -> new ItemResponse(
                            i.getProduct().getId(),
                            i.getProduct().getName(),
                            i.getQuantity(),
                            i.getUnitPrice(),
                            i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))
                    .toList();
            return new OrderResponse(o.getId(), o.getCustomer().getId(), o.getCustomer().getName(),
                    items, o.getTotal(), o.getStatus(), o.getCreatedAt());
        }
    }
}
