package inventory_api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 40) String sku,
        @NotNull @PositiveOrZero Integer quantity,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @Size(max = 60) String category,
        @Size(max = 100) String location,
        @PositiveOrZero Integer reorderPoint) { }
