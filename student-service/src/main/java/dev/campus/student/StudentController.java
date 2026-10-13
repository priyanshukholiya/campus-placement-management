package dev.campus.student;

import dev.campus.common.Contracts.StudentSnapshot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class StudentController {

  private final StudentRepository students;

  public StudentController(StudentRepository students) {
    this.students = students;
  }

  public record ProfileUpdate(
    @NotBlank @Size(max = 120) String name,
    @NotNull @Size(max = 1000) String skills
  ) {}

  public record AcademicUpdate(
    @NotBlank @Pattern(regexp = "CSE|IT|ECE|EE|ME|CE") String branch,
    @Min(2020) @Max(2100) int graduationYear,
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("10.0")
    @Digits(integer = 2, fraction = 2)
    BigDecimal cgpa,
    @Min(0) @Max(50) int activeBacklogs,
    boolean verified
  ) {}

  private Student get(String id) {
    return students
      .findById(id)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "Student profile not found."
        )
      );
  }

  @GetMapping("/api/students/me")
  @PreAuthorize("hasRole('STUDENT')")
  public Student me(Principal p) {
    return get(p.getName());
  }

  @PutMapping("/api/students/me")
  @PreAuthorize("hasRole('STUDENT')")
  @Transactional
  public Student update(
    Principal p,
    @Valid @RequestBody ProfileUpdate request
  ) {
    var s = get(p.getName());
    s.name = request.name().trim();
    s.skills = request.skills().trim();
    return students.save(s);
  }

  @GetMapping("/api/students")
  @PreAuthorize("hasRole('OFFICER')")
  public List<Student> all() {
    return students.findAll(org.springframework.data.domain.Sort.by("name"));
  }

  @PatchMapping("/api/students/{id}/academics")
  @PreAuthorize("hasRole('OFFICER')")
  @Transactional
  public Student academics(
    @PathVariable String id,
    @Valid @RequestBody AcademicUpdate r
  ) {
    var s = get(id);
    s.branch = r.branch();
    s.graduationYear = r.graduationYear();
    s.cgpa = r.cgpa();
    s.activeBacklogs = r.activeBacklogs();
    s.verified = r.verified();
    return students.save(s);
  }

  @GetMapping("/internal/students/{id}")
  public StudentSnapshot snapshot(@PathVariable String id) {
    var s = get(id);
    return new StudentSnapshot(
      s.id,
      s.name,
      s.branch,
      s.graduationYear,
      s.cgpa,
      s.activeBacklogs,
      s.verified
    );
  }

  @Bean
  CommandLineRunner seedStudents() {
    return args -> {
      if (students.count() == 0) {
        var a = new Student("student", "Aarav Sharma", "CSE", "8.40");
        a.skills = "Java, Spring Boot, SQL, React";
        var b = new Student("student2", "Meera Patel", "ECE", "6.20");
        b.skills = "Python, Embedded systems, SQL";
        students.saveAll(List.of(a, b));
      }
    };
  }
}
