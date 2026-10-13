package dev.campus.notification;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {

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

  public Instant readAt;

  protected Notification() {}
}
