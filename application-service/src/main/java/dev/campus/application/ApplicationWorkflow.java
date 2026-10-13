package dev.campus.application;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApplicationWorkflow {

  private final ApplicationRepository applications;
  private final HistoryRepository history;
  private final OutboxRepository outbox;
  private final ServiceClients clients;

  public ApplicationWorkflow(
    ApplicationRepository a,
    HistoryRepository h,
    OutboxRepository o,
    ServiceClients c
  ) {
    applications = a;
    history = h;
    outbox = o;
    clients = c;
  }

  static boolean hasRole(Authentication a, String role) {
    return a
      .getAuthorities()
      .stream()
      .anyMatch(r -> r.getAuthority().equals("ROLE_" + role));
  }

  public List<PlacementApplication> visible(Authentication a) {
    if (
      hasRole(a, "OFFICER")
    ) return applications.findAllByOrderByAppliedAtDesc();
    if (
      hasRole(a, "RECRUITER")
    ) return applications.findAllByRecruiterUsernameOrderByAppliedAtDesc(
      a.getName()
    );
    return applications.findAllByStudentIdOrderByAppliedAtDesc(a.getName());
  }

  public PlacementApplication accessible(String id, Authentication auth) {
    var a = applications
      .findById(id)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "Application not found."
        )
      );
    if (
      !(
        hasRole(auth, "OFFICER") ||
        (hasRole(auth, "RECRUITER") &&
          a.recruiterUsername.equals(auth.getName())) ||
        (hasRole(auth, "STUDENT") && a.studentId.equals(auth.getName()))
      )
    ) throw new ResponseStatusException(
      HttpStatus.NOT_FOUND,
      "Application not found."
    );
    return a;
  }

  @Transactional
  public PlacementApplication submit(String studentId, String driveId) {
    if (
      applications.existsByStudentIdAndDriveId(studentId, driveId)
    ) throw new ResponseStatusException(
      HttpStatus.CONFLICT,
      "You have already applied to this drive."
    );
    var s = clients.student(studentId);
    var d = clients.drive(driveId);
    var now = Instant.now();
    var result = Eligibility.evaluate(s, d, now);
    if (!result.eligible()) throw new ResponseStatusException(
      HttpStatus.UNPROCESSABLE_ENTITY,
      String.join(" ", result.reasons())
    );
    var a = new PlacementApplication();
    a.id = UUID.randomUUID().toString();
    a.studentId = s.id();
    a.driveId = d.id();
    a.studentName = s.name();
    a.branch = s.branch();
    a.cgpa = s.cgpa();
    a.graduationYear = s.graduationYear();
    a.activeBacklogs = s.activeBacklogs();
    a.company = d.company();
    a.driveTitle = d.title();
    a.recruiterUsername = d.recruiterUsername();
    a.eligibilitySnapshot =
      "Verified academics; branches=" +
      String.join(",", d.branches()) +
      "; graduationYear=" +
      d.graduationYear() +
      "; minCgpa=" +
      d.minCgpa() +
      "; maxBacklogs=" +
      d.maxBacklogs() +
      "; deadline=" +
      d.deadline() +
      "; status=" +
      d.status();
    a.status = ApplicationStatus.SUBMITTED;
    a.appliedAt = now;
    a.updatedAt = now;
    applications.saveAndFlush(a);
    record(a, studentId, "Application submitted.");
    return a;
  }

  @Transactional
  public PlacementApplication change(
    String id,
    ApplicationController.ChangeStatus r,
    Authentication auth
  ) {
    var a = accessible(id, auth);
    if (
      hasRole(auth, "STUDENT") && r.status() != ApplicationStatus.WITHDRAWN
    ) throw new ResponseStatusException(
      HttpStatus.FORBIDDEN,
      "Students can only withdraw their own applications."
    );
    if (!a.status.canMoveTo(r.status())) throw new ResponseStatusException(
      HttpStatus.CONFLICT,
      "Cannot move from " + a.status + " to " + r.status() + "."
    );
    if (r.status() == ApplicationStatus.INTERVIEW_SCHEDULED) {
      if (
        r.interviewAt() == null || !r.interviewAt().isAfter(Instant.now())
      ) throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Choose a future interview time."
      );
      if (r.meetingUrl() == null) throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "An interview meeting URL is required."
      );
      try {
        var u = URI.create(r.meetingUrl());
        if (
          !(
            "http".equalsIgnoreCase(u.getScheme()) ||
            "https".equalsIgnoreCase(u.getScheme())
          ) ||
          u.getHost() == null
        ) throw new IllegalArgumentException();
      } catch (IllegalArgumentException e) {
        throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Use an http or https meeting URL."
        );
      }
      a.interviewAt = r.interviewAt();
      a.meetingUrl = r.meetingUrl();
    }
    a.status = r.status();
    a.updatedAt = Instant.now();
    applications.saveAndFlush(a);
    record(a, auth.getName(), r.note() == null ? "" : r.note().trim());
    return a;
  }

  private void record(PlacementApplication a, String actor, String note) {
    var h = new StatusHistory();
    h.id = UUID.randomUUID().toString();
    h.applicationId = a.id;
    h.status = a.status.name();
    h.changedBy = actor;
    h.note = note;
    h.changedAt = a.updatedAt;
    history.save(h);
    var e = new OutboxEvent();
    e.id = UUID.randomUUID().toString();
    e.recipient = a.studentId;
    e.title = "Application " + a.status.name().toLowerCase().replace('_', ' ');
    e.message =
      a.company +
      " · " +
      a.driveTitle +
      " — " +
      a.status.name().replace('_', ' ') +
      (a.status == ApplicationStatus.INTERVIEW_SCHEDULED
        ? " at " + a.interviewAt
        : "");
    e.occurredAt = a.updatedAt;
    outbox.save(e);
  }
}
