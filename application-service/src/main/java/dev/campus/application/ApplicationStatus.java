package dev.campus.application;

public enum ApplicationStatus {
  SUBMITTED,
  SHORTLISTED,
  INTERVIEW_SCHEDULED,
  SELECTED,
  REJECTED,
  WITHDRAWN;

  public boolean canMoveTo(ApplicationStatus next) {
    return switch (this) {
      case SUBMITTED -> next == SHORTLISTED ||
        next == REJECTED ||
        next == WITHDRAWN;
      case SHORTLISTED -> next == INTERVIEW_SCHEDULED ||
        next == REJECTED ||
        next == WITHDRAWN;
      case INTERVIEW_SCHEDULED -> next == SELECTED ||
        next == REJECTED ||
        next == WITHDRAWN;
      default -> false;
    };
  }
}
