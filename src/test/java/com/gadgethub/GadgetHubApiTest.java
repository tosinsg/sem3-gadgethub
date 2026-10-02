package com.gadgethub;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** End-to-end test: HTTP -> Controller -> Service -> Repository -> H2 database. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GadgetHubApiTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    // ---------- helpers ----------
    private long createProduct(String name, int stock) throws Exception {
        String body = """
                {"name":"%s","category":"Audio","brand":"Test","price":50.00,"stock":%d,"description":"d"}
                """.formatted(name, stock);
        String res = mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(res).get("id").asLong();
    }

    private long createCustomer(String email) throws Exception {
        String body = """
                {"name":"Test User","email":"%s"}
                """.formatted(email);
        String res = mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(res).get("id").asLong();
    }

    private String orderJson(long customerId, long productId, int qty) {
        return """
                {"customerId":%d,"items":[{"productId":%d,"quantity":%d}]}
                """.formatted(customerId, productId, qty);
    }

    private long placeOrder(long customerId, long productId, int qty) throws Exception {
        String res = mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(customerId, productId, qty)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(res).get("id").asLong();
    }

    private long stockOf(long productId) throws Exception {
        String res = mvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(res).get("stock").asLong();
    }

    // ---------- tests ----------
    @Test
    void placingOrderReducesStock_andCancelRestoresIt() throws Exception {
        long productId = createProduct("Earbuds A", 10);
        long customerId = createCustomer("flow@test.com");

        String res = mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(customerId, productId, 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.total").value(150.0))
                .andReturn().getResponse().getContentAsString();
        long orderId = mapper.readTree(res).get("id").asLong();
        assertEquals(7, stockOf(productId));

        mvc.perform(patch("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertEquals(10, stockOf(productId));
    }

    @Test
    void insufficientStock_returns409_andLeavesStockUntouched() throws Exception {
        long productId = createProduct("Charger B", 2);
        long customerId = createCustomer("stock@test.com");

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(customerId, productId, 5)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Insufficient stock")));

        assertEquals(2, stockOf(productId));
    }

    @Test
    void invalidInput_returns400() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"category\":\"Audio\",\"price\":-5,\"stock\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.name").exists());

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":1,\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownProduct_returns404() throws Exception {
        mvc.perform(get("/api/products/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void duplicateEmail_returns409() throws Exception {
        createCustomer("dup@test.com");
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Other\",\"email\":\"dup@test.com\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void productWithOrders_cannotBeDeleted_butUnusedOneCan() throws Exception {
        long usedId = createProduct("Mouse C", 5);
        long customerId = createCustomer("del@test.com");
        placeOrder(customerId, usedId, 1);

        mvc.perform(delete("/api/products/{id}", usedId)).andExpect(status().isConflict());

        long freeId = createProduct("Mouse D", 5);
        mvc.perform(delete("/api/products/{id}", freeId)).andExpect(status().isNoContent());
    }

    @Test
    void shippedOrder_cannotBeCancelled() throws Exception {
        long productId = createProduct("Watch E", 5);
        long customerId = createCustomer("ship@test.com");
        long orderId = placeOrder(customerId, productId, 1);

        mvc.perform(patch("/api/orders/{id}/ship", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        mvc.perform(patch("/api/orders/{id}/cancel", orderId)).andExpect(status().isConflict());
    }
}
