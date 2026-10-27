package inventory_api.config;

import inventory_api.model.Product;
import inventory_api.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoDataLoader {
    @Bean
    @ConditionalOnProperty(name = "inventory.demo-data.enabled", havingValue = "true", matchIfMissing = true)
    ApplicationRunner addDemoProducts(ProductRepository products) {
        return args -> {
            if (products.count() > 0) return;
            products.saveAll(List.of(
                    new Product("Wireless Headphones", "AUD-2048", 42, new BigDecimal("79.99"), "Electronics", "Aisle 04 · Bay B", 12),
                    new Product("Stainless Water Bottle", "HOM-1102", 8, new BigDecimal("24.50"), "Home & Kitchen", "Aisle 12 · Bay A", 15),
                    new Product("Everyday Backpack", "APP-4310", 27, new BigDecimal("54.00"), "Apparel", "Aisle 08 · Bay C", 10),
                    new Product("LED Desk Lamp", "HOM-2033", 5, new BigDecimal("32.99"), "Home & Kitchen", "Aisle 12 · Bay D", 10),
                    new Product("USB-C Charging Cable", "ELC-0821", 116, new BigDecimal("12.99"), "Electronics", "Aisle 04 · Bay A", 25),
                    new Product("Cotton Crew T-Shirt", "APP-3308", 63, new BigDecimal("18.00"), "Apparel", "Aisle 08 · Bay A", 20),
                    new Product("Ceramic Coffee Mug", "HOM-1901", 3, new BigDecimal("9.50"), "Home & Kitchen", "Aisle 12 · Bay B", 12),
                    new Product("Bluetooth Speaker", "AUD-2250", 19, new BigDecimal("49.95"), "Electronics", "Aisle 04 · Bay C", 8)
            ));
        };
    }
}
