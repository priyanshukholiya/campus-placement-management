package dev.campus.notification;

import dev.campus.common.Contracts.NotificationEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Inbox {

  private final NotificationRepository notifications;

  public Inbox(NotificationRepository notifications) {
    this.notifications = notifications;
  }

  @Transactional
  public void accept(NotificationEvent e) {
    if (notifications.existsById(e.eventId())) return;
    var n = new Notification();
    n.id = e.eventId();
    n.recipient = e.recipient();
    n.title = e.title();
    n.message = e.message();
    n.occurredAt = e.occurredAt();
    notifications.saveAndFlush(n);
  }
}
