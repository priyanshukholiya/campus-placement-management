package dev.campus.application;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
  name = "placement_applications",
  uniqueConstraints = @UniqueConstraint(
    columnNames = { "student_id", "drive_id" }
  )
)
public class PlacementApplication {

  @Id
  @Column(length = 36)
  public String id;

  @Column(nullable = false, length = 80)
  public String studentId;

  @Column(nullable = false, length = 36)
  public String driveId;

  @Column(nullable = false, length = 120)
  public String studentName;

  @Column(nullable = false, length = 20)
  public String branch;

  @Column(precision = 4, scale = 2, nullable = false)
  public BigDecimal cgpa;

  public int graduationYear;
  public int activeBacklogs;

  @Column(nullable = false, length = 120)
  public String company;

  @Column(nullable = false, length = 160)
  public String driveTitle;

  @Column(nullable = false, length = 80)
  public String recruiterUsername;

  @Column(nullable = false, length = 1000)
  public String eligibilitySnapshot;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  public ApplicationStatus status;

  public Instant interviewAt;

  @Column(length = 500)
  public String meetingUrl;

  @Column(nullable = false)
  public Instant appliedAt;

  @Column(nullable = false)
  public Instant updatedAt;

  @Version
  public long version;

  protected PlacementApplication() {}
}
