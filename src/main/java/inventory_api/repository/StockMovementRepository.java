package inventory_api.repository;

import inventory_api.model.StockMovement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);
    List<StockMovement> findTop10ByOrderByCreatedAtDesc();
    boolean existsByProductId(Long productId);
}
