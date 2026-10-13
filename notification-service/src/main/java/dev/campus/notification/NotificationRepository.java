package dev.campus.notification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository
  extends JpaRepository<Notification, String>
{
  List<Notification> findAllByRecipientOrderByOccurredAtDesc(String recipient);
}
