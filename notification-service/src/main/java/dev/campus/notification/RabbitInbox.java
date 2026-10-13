package dev.campus.notification;

import dev.campus.common.Contracts.NotificationEvent;
import jakarta.validation.Validator;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "campus.messaging", havingValue = "rabbit")
public class RabbitInbox {

  private final Inbox inbox;
  private final NotificationRepository repo;
  private final ObjectMapper json;
  private final Validator validator;

  public RabbitInbox(
    Inbox inbox,
    NotificationRepository repo,
    ObjectMapper json,
    Validator validator
  ) {
    this.inbox = inbox;
    this.repo = repo;
    this.json = json;
    this.validator = validator;
  }

  @RabbitListener(queues = "campus.notifications")
  public void receive(String payload) {
    var e = json.readValue(payload, NotificationEvent.class);
    if (!validator.validate(e).isEmpty()) throw new IllegalArgumentException(
      "Invalid notification event."
    );
    try {
      inbox.accept(e);
    } catch (DataIntegrityViolationException conflict) {
      if (!repo.existsById(e.eventId())) throw conflict;
    }
  }
}
