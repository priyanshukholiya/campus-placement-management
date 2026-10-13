package dev.campus.application;

import dev.campus.common.ApiErrors.ErrorBody;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasAnyRole('STUDENT','OFFICER','RECRUITER')")
public class ApplicationController {

  private final ApplicationWorkflow workflow;
  private final ServiceClients clients;
  private final HistoryRepository history;
  private final OutboxRepository outbox;

  public ApplicationController(
    ApplicationWorkflow w,
    ServiceClients c,
    HistoryRepository h,
    OutboxRepository o
  ) {
    workflow = w;
    clients = c;
    history = h;
    outbox = o;
  }

  public record Submission(@NotBlank @Size(max = 36) String driveId) {}

  public record ChangeStatus(
    @NotNull ApplicationStatus status,
    @Size(max = 500) String note,
    Instant interviewAt,
    @Size(max = 500) String meetingUrl
  ) {}

  @GetMapping("/api/applications")
  public List<PlacementApplication> all(Authentication a) {
    return workflow.visible(a);
  }

  @GetMapping("/api/applications/eligibility/{driveId}")
  @PreAuthorize("hasRole('STUDENT')")
  public Eligibility.Result eligibility(
    @PathVariable String driveId,
    Authentication a
  ) {
    return Eligibility.evaluate(
      clients.student(a.getName()),
      clients.drive(driveId),
      Instant.now()
    );
  }

  @PostMapping("/api/applications")
  @PreAuthorize("hasRole('STUDENT')")
  @ResponseStatus(HttpStatus.CREATED)
  public PlacementApplication submit(
    @Valid @RequestBody Submission r,
    Authentication a
  ) {
    return workflow.submit(a.getName(), r.driveId());
  }

  @PatchMapping("/api/applications/{id}/status")
  public PlacementApplication change(
    @PathVariable String id,
    @Valid @RequestBody ChangeStatus r,
    Authentication a
  ) {
    return workflow.change(id, r, a);
  }

  @GetMapping("/api/applications/{id}/history")
  public List<StatusHistory> history(
    @PathVariable String id,
    Authentication a
  ) {
    workflow.accessible(id, a);
    return history.findAllByApplicationIdOrderByChangedAtAsc(id);
  }

  @GetMapping("/api/reports")
  @PreAuthorize("hasAnyRole('OFFICER','RECRUITER')")
  public Map<String, Object> report(Authentication auth) {
    var list = workflow.visible(auth);
    return Map.of(
      "applications",
      list.size(),
      "offers",
      list
        .stream()
        .filter(a -> a.status == ApplicationStatus.SELECTED)
        .count(),
      "placedStudents",
      list
        .stream()
        .filter(a -> a.status == ApplicationStatus.SELECTED)
        .map(a -> a.studentId)
        .distinct()
        .count(),
      "byStatus",
      list
        .stream()
        .collect(Collectors.groupingBy(a -> a.status, Collectors.counting()))
    );
  }

  @GetMapping("/api/outbox")
  @PreAuthorize("hasRole('OFFICER')")
  public Map<String, Long> outbox() {
    return Map.of(
      "pending",
      outbox.countByDeliveryStatus("PENDING"),
      "delivered",
      outbox.countByDeliveryStatus("DELIVERED"),
      "failed",
      outbox.countByDeliveryStatus("FAILED")
    );
  }

  @PostMapping("/api/outbox/retry")
  @PreAuthorize("hasRole('OFFICER')")
  public Map<String, Integer> retry() {
    var events = outbox.findAllByDeliveryStatus("FAILED");
    events.forEach(e -> {
      e.deliveryStatus = "PENDING";
      e.attempts = 0;
      e.nextAttemptAt = null;
      e.lastError = null;
    });
    outbox.saveAll(events);
    return Map.of("retried", events.size());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ErrorBody> duplicate() {
    return ResponseEntity.status(409).body(
      new ErrorBody(
        "CONFLICT",
        "An application already exists for this student and drive.",
        List.of()
      )
    );
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  ResponseEntity<ErrorBody> concurrent() {
    return ResponseEntity.status(409).body(
      new ErrorBody(
        "CONCURRENT_UPDATE",
        "This application changed. Refresh and try again.",
        List.of()
      )
    );
  }
}
