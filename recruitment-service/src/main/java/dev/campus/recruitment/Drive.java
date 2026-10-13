package dev.campus.recruitment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "drives")
public class Drive {

  @Id
  @Column(length = 36)
  public String id;

  @Column(nullable = false, length = 120)
  public String company;

  @Column(nullable = false, length = 160)
  public String title;

  @Column(nullable = false, length = 100)
  public String location;

  @Column(nullable = false, length = 2000)
  public String description;

  @Column(nullable = false, length = 80)
  public String recruiterUsername;

  @Column(nullable = false, length = 100)
  public String branches;

  public int graduationYear;

  @Column(precision = 4, scale = 2, nullable = false)
  public BigDecimal minCgpa;

  public int maxBacklogs;

  @Column(precision = 6, scale = 2, nullable = false)
  public BigDecimal packageLpa;

  @Column(nullable = false)
  public Instant deadline;

  @Column(nullable = false, length = 20)
  public String status;

  @Column(nullable = false)
  public Instant createdAt;

  @Version
  public long version;

  protected Drive() {}
}
