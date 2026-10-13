package dev.campus.application;

import dev.campus.common.Contracts.NotificationEvent;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Component
@EnableScheduling
@ConditionalOnProperty(
  name = "campus.outbox-enabled",
  havingValue = "true",
  matchIfMissing = true
)
public class OutboxDispatcher {

  private final OutboxRepository outbox;
  private final RestClient http;
  private final String mode;
  private final ObjectProvider<RabbitTemplate> rabbit;
  private final ObjectMapper json;

  public OutboxDispatcher(
    OutboxRepository outbox,
    @Value("${campus.notifications-url}") String url,
    @Value("${campus.service-password}") String password,
    @Value("${campus.messaging:http}") String mode,
    ObjectProvider<RabbitTemplate> rabbit,
    ObjectMapper json
  ) {
    this.outbox = outbox;
    this.http = ServiceClients.client(url, password);
    this.mode = mode;
    this.rabbit = rabbit;
    this.json = json;
  }

  @Scheduled(fixedDelayString = "${campus.outbox-delay:2000}")
  public void deliver() {
    for (var e : outbox.findTop20ByDeliveryStatusOrderByOccurredAtAsc(
      "PENDING"
    )) {
      if (
        e.nextAttemptAt != null && e.nextAttemptAt.isAfter(Instant.now())
      ) continue;
      try {
        var event = new NotificationEvent(
          e.id,
          e.recipient,
          e.title,
          e.message,
          e.occurredAt
        );
        if ("rabbit".equals(mode)) {
          var correlation = new CorrelationData(e.id);
          rabbit
            .getObject()
            .convertAndSend(
              "campus.events",
              "application.changed",
              json.writeValueAsString(event),
              correlation
            );
          var confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
          if (
            !confirm.ack() || correlation.getReturned() != null
          ) throw new IllegalStateException("Broker did not accept event.");
        } else http
          .post()
          .uri("/internal/notifications")
          .body(event)
          .retrieve()
          .toBodilessEntity();
        e.deliveryStatus = "DELIVERED";
        e.lastError = null;
      } catch (Exception ex) {
        e.attempts++;
        e.lastError = "Delivery failed: " + ex.getClass().getSimpleName();
        e.nextAttemptAt = Instant.now().plusSeconds(
          Math.min(60, 1L << Math.min(e.attempts, 6))
        );
        if (e.attempts >= 8) e.deliveryStatus = "FAILED";
        if (
          ex instanceof InterruptedException
        ) Thread.currentThread().interrupt();
      }
      outbox.save(e);
    }
  }
}
