package inventory_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import inventory_api.model.Product;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class InventoryAssistantServiceTests {

    private final InventoryAssistantService assistant = new InventoryAssistantService(
            mock(ProductService.class), "", "test-model", "http://localhost/unused");

    @Test
    void suggestsQuantityThatRaisesLowStockToTwiceItsReorderPoint() {
        Product lowStock = product("LAMP-01", 3, 10);

        assertEquals(17L, InventoryAssistantService.recommendedOrderQuantity(lowStock));
    }

    @Test
    void suggestsNoOrderForProductsAboveTheirReorderPoint() {
        Product healthyStock = product("MUG-02", 12, 10);

        assertEquals(0L, InventoryAssistantService.recommendedOrderQuantity(healthyStock));
    }

    @Test
    void snapshotGivesTheAssistantGroundedProductAndRestockEvidence() {
        Product lowStock = new Product("Desk Lamp", "LAMP-01", 3, new BigDecimal("24.50"),
                "Lighting", "Aisle 4", 10);

        String snapshot = assistant.buildInventorySnapshot(List.of(lowStock));

        assertTrue(snapshot.contains("SKU LAMP-01"));
        assertTrue(snapshot.contains("Desk Lamp"));
        assertTrue(snapshot.contains("category Lighting"));
        assertTrue(snapshot.contains("location Aisle 4"));
        assertTrue(snapshot.contains("on hand 3 | reorder point 10"));
        assertTrue(snapshot.contains("restock status REORDER"));
        assertTrue(snapshot.contains("suggested order quantity 17"));
    }

    @Test
    void emptyInventoryProducesAnExplicitEmptySnapshot() {
        assertEquals("No products are currently recorded.", assistant.buildInventorySnapshot(List.of()));
    }

    private Product product(String sku, int quantity, int reorderPoint) {
        return new Product("Test product", sku, quantity, new BigDecimal("1.00"), "Test", "Test location", reorderPoint);
    }
}
