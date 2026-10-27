package inventory_api.service;

import inventory_api.dto.ProductRequest;
import inventory_api.dto.StockMovementResponse;
import inventory_api.exception.ConflictException;
import inventory_api.exception.ResourceNotFoundException;
import inventory_api.model.Product;
import inventory_api.model.StockMovement;
import inventory_api.repository.ProductRepository;
import inventory_api.repository.StockMovementRepository;
import java.util.List;
import java.util.Comparator;
import java.util.PriorityQueue;
import inventory_api.dto.StockActivityResponse;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService {
    private final ProductRepository products;
    private final StockMovementRepository movements;

    public ProductService(ProductRepository products, StockMovementRepository movements) {
        this.products = products;
        this.movements = movements;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() { return products.findAll(); }

    @Transactional(readOnly = true)
    public List<Product> restockRecommendations() {
        PriorityQueue<Product> urgentFirst = new PriorityQueue<>(Comparator.comparingInt(Product::getQuantity));
        products.findAll().stream().filter(p -> p.getQuantity() <= p.getReorderPoint()).forEach(urgentFirst::offer);
        List<Product> result = new java.util.ArrayList<>();
        while (!urgentFirst.isEmpty()) result.add(urgentFirst.poll());
        return result;
    }

    @Transactional(readOnly = true)
    public List<StockActivityResponse> recentActivity() {
        return movements.findTop10ByOrderByCreatedAtDesc().stream()
                .map(m -> new StockActivityResponse(m.getId(), m.getProduct().getId(), m.getProduct().getName(),
                        m.getProduct().getSku(), m.getQuantityChange(), m.getQuantityAfter(), m.getReason(), m.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) { return getProduct(id); }

    public Product create(ProductRequest request) {
        String sku = normalizeSku(request.sku());
        if (products.existsBySku(sku)) throw new ConflictException("A product with this SKU already exists");
        return products.save(new Product(request.name().trim(), sku, request.quantity(), request.price(),
                defaultText(request.category(), "General"), defaultText(request.location(), "Main warehouse"),
                request.reorderPoint() == null ? 10 : request.reorderPoint()));
    }

    public Product update(Long id, ProductRequest request) {
        Product product = getProduct(id);
        String sku = normalizeSku(request.sku());
        if (products.existsBySkuAndIdNot(sku, id)) throw new ConflictException("A product with this SKU already exists");
        product.setName(request.name().trim());
        product.setSku(sku);
        product.setQuantity(request.quantity());
        product.setPrice(request.price());
        product.setCategory(defaultText(request.category(), "General"));
        product.setLocation(defaultText(request.location(), "Main warehouse"));
        product.setReorderPoint(request.reorderPoint() == null ? 10 : request.reorderPoint());
        return products.save(product);
    }

    public void delete(Long id) {
        Product product = getProduct(id);
        if (movements.existsByProductId(id)) throw new ConflictException("A product with stock history cannot be deleted");
        products.delete(product);
    }

    public Product updateStock(Long id, int change, String reason) {
        if (change == 0) throw new IllegalArgumentException("quantityChange must not be zero");
        Product product = getProduct(id);
        long updated = (long) product.getQuantity() + change;
        if (updated < 0 || updated > Integer.MAX_VALUE) throw new IllegalArgumentException("Stock must remain between 0 and 2147483647");
        product.setQuantity((int) updated);
        products.save(product);
        movements.save(new StockMovement(product, change, product.getQuantity(), normalizeReason(reason)));
        return product;
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> stockHistory(Long id) {
        getProduct(id);
        return movements.findByProductIdOrderByCreatedAtDesc(id).stream()
                .map(m -> new StockMovementResponse(m.getId(), id, m.getQuantityChange(), m.getQuantityAfter(), m.getReason(), m.getCreatedAt()))
                .toList();
    }

    private Product getProduct(Long id) {
        return products.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product " + id + " was not found"));
    }
    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) return "Manual adjustment";
        return reason.trim().substring(0, Math.min(reason.trim().length(), 200));
    }
    private String defaultText(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
    private String normalizeSku(String value) { return value.trim().toUpperCase(java.util.Locale.ROOT); }
}
