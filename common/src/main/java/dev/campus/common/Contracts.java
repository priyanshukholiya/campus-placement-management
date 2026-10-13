package dev.campus.common;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class Contracts {

  private Contracts() {}

  public record StudentSnapshot(
    String id,
    String name,
    String branch,
    int graduationYear,
    BigDecimal cgpa,
    int activeBacklogs,
    boolean verified
  ) {}

  public record DriveSnapshot(
    String id,
    String company,
    String title,
    String recruiterUsername,
    List<String> branches,
    int graduationYear,
    BigDecimal minCgpa,
    int maxBacklogs,
    Instant deadline,
    String status
  ) {}

  public record NotificationEvent(
    @NotBlank @Size(max = 36) String eventId,
    @NotBlank @Size(max = 80) String recipient,
    @NotBlank @Size(max = 160) String title,
    @NotBlank @Size(max = 1000) String message,
    @NotNull Instant occurredAt
  ) {}
}
