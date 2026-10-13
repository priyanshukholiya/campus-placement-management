package dev.campus.application;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "status_history")
public class StatusHistory {

  @Id
  @Column(length = 36)
  public String id;

  @Column(nullable = false, length = 36)
  public String applicationId;

  @Column(nullable = false, length = 30)
  public String status;

  @Column(nullable = false, length = 80)
  public String changedBy;

  @Column(nullable = false, length = 500)
  public String note;

  @Column(nullable = false)
  public Instant changedAt;

  protected StatusHistory() {}
}
