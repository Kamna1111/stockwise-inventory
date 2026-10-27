package inventory_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inventory_api.dto.ProductRequest;
import inventory_api.exception.ConflictException;
import inventory_api.model.Product;
import inventory_api.repository.ProductRepository;
import inventory_api.repository.StockMovementRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTests {
    @Mock ProductRepository products;
    @Mock StockMovementRepository movements;
    ProductService service;

    @BeforeEach
    void setUp() { service = new ProductService(products, movements); }

    @Test
    void refusesDuplicateSkuBeforeSaving() {
        when(products.existsBySku("LAP-001")).thenReturn(true);
        ProductRequest request = new ProductRequest("Laptop", "LAP-001", 4, new BigDecimal("10.00"), null, null, null);
        assertThrows(ConflictException.class, () -> service.create(request));
        verify(products, never()).save(any());
    }

    @Test
    void rejectsStockChangeThatWouldGoBelowZero() {
        Product product = new Product("Mouse", "MOU-001", 2, new BigDecimal("5.00"), "Electronics", "Aisle 1", 5);
        when(products.findById(1L)).thenReturn(Optional.of(product));
        assertThrows(IllegalArgumentException.class, () -> service.updateStock(1L, -3, "Sale"));
        verify(products, never()).save(any());
        verify(movements, never()).save(any());
    }

    @Test
    void recordsTheNewStockLevelAndMovement() {
        Product product = new Product("Mouse", "MOU-001", 2, new BigDecimal("5.00"), "Electronics", "Aisle 1", 5);
        when(products.findById(1L)).thenReturn(Optional.of(product));
        when(products.save(product)).thenReturn(product);
        Product updated = service.updateStock(1L, 4, "Shipment");
        assertEquals(6, updated.getQuantity());
        verify(movements).save(any());
    }

    @Test
    void usesPriorityQueueToOrderRestockItemsByUrgency() {
        Product urgent = new Product("Almost gone", "A", 1, new BigDecimal("1.00"), "General", "Main", 5);
        Product soon = new Product("Running low", "B", 4, new BigDecimal("1.00"), "General", "Main", 5);
        Product healthy = new Product("Healthy", "C", 20, new BigDecimal("1.00"), "General", "Main", 5);
        when(products.findAll()).thenReturn(List.of(soon, healthy, urgent));
        List<Product> queue = service.restockRecommendations();
        assertEquals(List.of("A", "B"), queue.stream().map(Product::getSku).toList());
    }
}
