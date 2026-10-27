package inventory_api.dto;

import jakarta.validation.constraints.NotNull;

public record StockUpdateRequest(@NotNull Integer quantityChange, String reason) { }
