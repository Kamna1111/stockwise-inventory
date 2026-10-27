package inventory_api.dto;

import java.time.Instant;

public record StockMovementResponse(Long id, Long productId, int change, int quantityAfter, String reason, Instant createdAt) { }
