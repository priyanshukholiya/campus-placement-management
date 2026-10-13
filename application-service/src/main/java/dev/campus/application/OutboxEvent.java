package dev.campus.application;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

  @Id
  @Column(length = 36)
  public String id;

  @Column(nullable = false, length = 80)
  public String recipient;

  @Column(nullable = false, length = 160)
  public String title;

  @Column(nullable = false, length = 1000)
  public String message;

  @Column(nullable = false)
  public Instant occurredAt;

  @Column(nullable = false, length = 20)
  public String deliveryStatus = "PENDING";

  public int attempts;
  public Instant nextAttemptAt;

  @Column(length = 500)
  public String lastError;

  protected OutboxEvent() {}
}
