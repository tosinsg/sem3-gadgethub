package com.gadgethub.config;

import com.gadgethub.model.Customer;
import com.gadgethub.model.Product;
import com.gadgethub.repository.CustomerRepository;
import com.gadgethub.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Loads demo data on startup (skipped in tests via the "test" profile). */
@Component
@Profile("!test")
public class DataLoader implements CommandLineRunner {

    private final ProductRepository products;
    private final CustomerRepository customers;

    public DataLoader(ProductRepository products, CustomerRepository customers) {
        this.products = products;
        this.customers = customers;
    }

    @Override
    public void run(String... args) {
        if (products.count() == 0) {
            products.saveAll(List.of(
                    item("Wireless Earbuds Pro", "Audio", "SoundWave", "59.99", 50),
                    item("Bluetooth Speaker Mini", "Audio", "SoundWave", "34.50", 40),
                    item("Smart Watch S2", "Wearables", "TechFit", "129.00", 25),
                    item("Fitness Band Lite", "Wearables", "TechFit", "39.99", 60),
                    item("USB-C Fast Charger 65W", "Chargers", "VoltEdge", "24.99", 100),
                    item("Power Bank 20000mAh", "Chargers", "VoltEdge", "29.99", 80),
                    item("Mechanical Keyboard", "Computer Accessories", "KeyForge", "79.00", 30),
                    item("Gaming Mouse RGB", "Computer Accessories", "KeyForge", "27.50", 45)));
        }
        if (customers.count() == 0) {
            Customer c = new Customer();
            c.setName("Demo Customer");
            c.setEmail("demo@gadgethub.com");
            customers.save(c);
        }
    }

    private Product item(String name, String category, String brand, String price, int stock) {
        Product p = new Product();
        p.setName(name);
        p.setCategory(category);
        p.setBrand(brand);
        p.setPrice(new BigDecimal(price));
        p.setStock(stock);
        p.setDescription(name + " by " + brand);
        return p;
    }
}
