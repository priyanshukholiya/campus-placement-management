package dev.campus.student;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "students")
public class Student {

  @Id
  @Column(length = 80)
  public String id;

  @Column(nullable = false, length = 120)
  public String name;

  @Column(nullable = false, length = 20)
  public String branch;

  public int graduationYear;

  @Column(precision = 4, scale = 2, nullable = false)
  public BigDecimal cgpa;

  public int activeBacklogs;
  public boolean verified;

  @Column(nullable = false, length = 1000)
  public String skills = "";

  @Version
  public long version;

  protected Student() {}

  Student(String id, String name, String branch, String cgpa) {
    this.id = id;
    this.name = name;
    this.branch = branch;
    this.cgpa = new BigDecimal(cgpa);
    this.graduationYear = 2027;
    this.verified = true;
  }
}
