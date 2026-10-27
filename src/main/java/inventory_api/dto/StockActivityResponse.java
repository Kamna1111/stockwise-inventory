package inventory_api.dto;

import java.time.Instant;

public record StockActivityResponse(Long id, Long productId, String productName, String sku,
                                    int change, int quantityAfter, String reason, Instant createdAt) { }
