package dev.campus.application;

import dev.campus.common.Contracts.*;
import java.time.Instant;
import java.util.*;

public final class Eligibility {

  private Eligibility() {}

  public record Result(boolean eligible, List<String> reasons) {}

  public static Result evaluate(
    StudentSnapshot s,
    DriveSnapshot d,
    Instant now
  ) {
    var reasons = new ArrayList<String>();
    if (!"PUBLISHED".equals(d.status())) reasons.add("This drive is closed.");
    if (!d.deadline().isAfter(now)) reasons.add(
      "The application deadline has passed."
    );
    if (!s.verified()) reasons.add(
      "Your academic profile needs placement-officer verification."
    );
    if (!d.branches().contains(s.branch())) reasons.add(
      "Eligible branches: " + String.join(", ", d.branches()) + "."
    );
    if (s.graduationYear() != d.graduationYear()) reasons.add(
      "This drive is for the " + d.graduationYear() + " graduating batch."
    );
    if (s.cgpa().compareTo(d.minCgpa()) < 0) reasons.add(
      "Minimum CGPA is " + d.minCgpa().toPlainString() + "."
    );
    if (s.activeBacklogs() > d.maxBacklogs()) reasons.add(
      "Maximum active backlogs: " + d.maxBacklogs() + "."
    );
    return new Result(reasons.isEmpty(), List.copyOf(reasons));
  }
}
