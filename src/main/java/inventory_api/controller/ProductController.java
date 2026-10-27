package inventory_api.controller;

import inventory_api.dto.ProductRequest;
import inventory_api.dto.StockMovementResponse;
import inventory_api.dto.StockUpdateRequest;
import inventory_api.dto.StockActivityResponse;
import inventory_api.model.Product;
import inventory_api.service.ProductService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;
    public ProductController(ProductService service) { this.service = service; }

    @GetMapping public List<Product> getProducts() { return service.findAll(); }
    @GetMapping("/restock") public List<Product> getRestockRecommendations() { return service.restockRecommendations(); }
    @GetMapping("/activity") public List<StockActivityResponse> getRecentActivity() { return service.recentActivity(); }
    @GetMapping("/{id}") public Product getProduct(@PathVariable Long id) { return service.findById(id); }
    @PostMapping public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductRequest request) {
        Product created = service.create(request);
        return ResponseEntity.created(URI.create("/api/products/" + created.getId())).body(created);
    }
    @PutMapping("/{id}") public Product updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> deleteProduct(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }
    @PostMapping("/{id}/stock") public Product updateStock(@PathVariable Long id, @Valid @RequestBody StockUpdateRequest request) {
        return service.updateStock(id, request.quantityChange(), request.reason());
    }
    @GetMapping("/{id}/stock-history") public List<StockMovementResponse> getStockHistory(@PathVariable Long id) { return service.stockHistory(id); }
}
