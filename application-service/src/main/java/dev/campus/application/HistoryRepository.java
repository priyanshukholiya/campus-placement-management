package dev.campus.application;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoryRepository
  extends JpaRepository<StatusHistory, String>
{
  List<StatusHistory> findAllByApplicationIdOrderByChangedAtAsc(String id);
}
