package inventory_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import jakarta.persistence.Version;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @NotBlank
    @Size(max = 40)
    @Column(nullable = false, unique = true, length = 40)
    private String sku;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer quantity;

    @NotNull
    @DecimalMin(value = "0.00")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(length = 60)
    private String category;

    @Column(length = 100)
    private String location;

    @Column
    private Integer reorderPoint;

    @Version
    private Long version;

    protected Product() {}

    public Product(String name, String sku, Integer quantity, BigDecimal price, String category, String location, Integer reorderPoint) {
        this.name = name;
        this.sku = sku;
        this.quantity = quantity;
        this.price = price;
        this.category = category;
        this.location = location;
        this.reorderPoint = reorderPoint;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSku() { return sku; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public String getCategory() { return category == null || category.isBlank() ? "General" : category; }
    public String getLocation() { return location == null || location.isBlank() ? "Main warehouse" : location; }
    public Integer getReorderPoint() { return reorderPoint == null ? 10 : reorderPoint; }
    public void setName(String name) { this.name = name; }
    public void setSku(String sku) { this.sku = sku; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setCategory(String category) { this.category = category; }
    public void setLocation(String location) { this.location = location; }
    public void setReorderPoint(Integer reorderPoint) { this.reorderPoint = reorderPoint; }
}
