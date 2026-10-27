package inventory_api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "stock_movements")
public class StockMovement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Product product;
    private int quantityChange;
    private int quantityAfter;
    private String reason;
    private Instant createdAt;

    protected StockMovement() {}
    public StockMovement(Product product, int quantityChange, int quantityAfter, String reason) {
        this.product = product;
        this.quantityChange = quantityChange;
        this.quantityAfter = quantityAfter;
        this.reason = reason;
        this.createdAt = Instant.now();
    }
    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public int getQuantityChange() { return quantityChange; }
    public int getQuantityAfter() { return quantityAfter; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
