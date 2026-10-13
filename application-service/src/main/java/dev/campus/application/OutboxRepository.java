package dev.campus.application;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<OutboxEvent, String> {
  List<OutboxEvent> findTop20ByDeliveryStatusOrderByOccurredAtAsc(
    String status
  );
  List<OutboxEvent> findAllByDeliveryStatus(String status);
  long countByDeliveryStatus(String status);
}
