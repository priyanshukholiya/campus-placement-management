package dev.campus.application;

import static org.junit.jupiter.api.Assertions.*;

import dev.campus.common.Contracts.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class EligibilityTest {

  private final Instant now = Instant.parse("2026-10-13T00:00:00Z");

  private DriveSnapshot drive(Instant deadline) {
    return new DriveSnapshot(
      "d",
      "Aster",
      "Engineer",
      "recruiter",
      List.of("CSE"),
      2027,
      new BigDecimal("7.0"),
      0,
      deadline,
      "PUBLISHED"
    );
  }

  @Test
  void exactThresholdQualifies() {
    assertTrue(
      Eligibility.evaluate(
        new StudentSnapshot(
          "s",
          "Name",
          "CSE",
          2027,
          new BigDecimal("7.00"),
          0,
          true
        ),
        drive(now.plusSeconds(1)),
        now
      ).eligible()
    );
  }

  @Test
  void deadlineIsExclusive() {
    assertFalse(
      Eligibility.evaluate(
        new StudentSnapshot(
          "s",
          "Name",
          "CSE",
          2027,
          new BigDecimal("9"),
          0,
          true
        ),
        drive(now),
        now
      ).eligible()
    );
  }

  @Test
  void allFailuresAreExplained() {
    var r = Eligibility.evaluate(
      new StudentSnapshot(
        "s",
        "Name",
        "ME",
        2026,
        new BigDecimal("6"),
        2,
        false
      ),
      drive(now.minusSeconds(1)),
      now
    );
    assertFalse(r.eligible());
    assertEquals(6, r.reasons().size());
  }

  @Test
  void terminalStatesCannotReopen() {
    for (var state : List.of(
      ApplicationStatus.SELECTED,
      ApplicationStatus.REJECTED,
      ApplicationStatus.WITHDRAWN
    ))
      for (var next : ApplicationStatus.values())
        assertFalse(state.canMoveTo(next));
  }

  @Test
  void cannotSkipInterview() {
    assertFalse(
      ApplicationStatus.SUBMITTED.canMoveTo(ApplicationStatus.SELECTED)
    );
    assertTrue(
      ApplicationStatus.SHORTLISTED.canMoveTo(
        ApplicationStatus.INTERVIEW_SCHEDULED
      )
    );
  }
}
