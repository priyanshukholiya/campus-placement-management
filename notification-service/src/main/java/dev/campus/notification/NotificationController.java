package dev.campus.notification;

import dev.campus.common.Contracts.NotificationEvent;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class NotificationController {

  private final NotificationRepository notifications;
  private final Inbox inbox;

  public NotificationController(
    NotificationRepository notifications,
    Inbox inbox
  ) {
    this.notifications = notifications;
    this.inbox = inbox;
  }

  @GetMapping("/api/notifications")
  public List<Notification> mine(Principal p) {
    return notifications.findAllByRecipientOrderByOccurredAtDesc(p.getName());
  }

  @PatchMapping("/api/notifications/{id}/read")
  @Transactional
  public Notification read(@PathVariable String id, Principal p) {
    var n = notifications
      .findById(id)
      .filter(item -> item.recipient.equals(p.getName()))
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "Notification not found."
        )
      );
    n.readAt = Instant.now();
    return notifications.save(n);
  }

  @PostMapping("/internal/notifications")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void receive(@Valid @RequestBody NotificationEvent e) {
    try {
      inbox.accept(e);
    } catch (DataIntegrityViolationException conflict) {
      if (!notifications.existsById(e.eventId())) throw conflict;
    }
  }
}
