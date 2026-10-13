package dev.campus.recruitment;

import dev.campus.common.Contracts.DriveSnapshot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class DriveController {

  private final DriveRepository drives;

  public DriveController(DriveRepository drives) {
    this.drives = drives;
  }

  public record CreateDrive(
    @NotBlank @Size(max = 120) String company,
    @NotBlank @Size(max = 160) String title,
    @NotBlank @Size(max = 100) String location,
    @NotBlank @Size(max = 2000) String description,
    @NotBlank @Pattern(regexp = "recruiter") String recruiterUsername,
    @NotEmpty
    @Size(max = 6)
    List<@Pattern(regexp = "CSE|IT|ECE|EE|ME|CE") String> branches,
    @Min(2020) @Max(2100) int graduationYear,
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("10.0")
    @Digits(integer = 2, fraction = 2)
    BigDecimal minCgpa,
    @Min(0) @Max(50) int maxBacklogs,
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("9999.99")
    @Digits(integer = 4, fraction = 2)
    BigDecimal packageLpa,
    @NotNull @Future Instant deadline
  ) {}

  public record StatusUpdate(
    @NotBlank @Pattern(regexp = "PUBLISHED|CLOSED") String status
  ) {}

  private Drive get(String id) {
    return drives
      .findById(id)
      .orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Drive not found.")
      );
  }

  @GetMapping("/api/drives")
  public List<Drive> all() {
    return drives.findAll(
      org.springframework.data.domain.Sort.by(
        org.springframework.data.domain.Sort.Direction.DESC,
        "createdAt"
      )
    );
  }

  @GetMapping("/api/drives/{id}")
  public Drive one(@PathVariable String id) {
    return get(id);
  }

  @PostMapping("/api/drives")
  @PreAuthorize("hasRole('OFFICER')")
  @ResponseStatus(HttpStatus.CREATED)
  public Drive create(@Valid @RequestBody CreateDrive r) {
    return drives.save(from(r));
  }

  @PatchMapping("/api/drives/{id}/status")
  @PreAuthorize("hasRole('OFFICER')")
  @Transactional
  public Drive status(
    @PathVariable String id,
    @Valid @RequestBody StatusUpdate r
  ) {
    var d = get(id);
    if (
      r.status().equals("PUBLISHED") && !d.deadline.isAfter(Instant.now())
    ) throw new ResponseStatusException(
      HttpStatus.CONFLICT,
      "This drive's deadline has passed."
    );
    d.status = r.status();
    return drives.save(d);
  }

  @GetMapping("/internal/drives/{id}")
  public DriveSnapshot snapshot(@PathVariable String id) {
    var d = get(id);
    return new DriveSnapshot(
      d.id,
      d.company,
      d.title,
      d.recruiterUsername,
      List.of(d.branches.split(",")),
      d.graduationYear,
      d.minCgpa,
      d.maxBacklogs,
      d.deadline,
      d.status
    );
  }

  private Drive from(CreateDrive r) {
    var d = new Drive();
    d.id = UUID.randomUUID().toString();
    d.company = r.company().trim();
    d.title = r.title().trim();
    d.location = r.location().trim();
    d.description = r.description().trim();
    d.recruiterUsername = r.recruiterUsername();
    d.branches = String.join(",", new LinkedHashSet<>(r.branches()));
    d.graduationYear = r.graduationYear();
    d.minCgpa = r.minCgpa();
    d.maxBacklogs = r.maxBacklogs();
    d.packageLpa = r.packageLpa();
    d.deadline = r.deadline();
    d.status = "PUBLISHED";
    d.createdAt = Instant.now();
    return d;
  }

  @Bean
  CommandLineRunner seedDrives() {
    return args -> {
      if (drives.count() == 0) {
        var now = Instant.now();
        drives.save(
          from(
            new CreateDrive(
              "Aster Labs",
              "Graduate Software Engineer",
              "Bengaluru · Hybrid",
              "Build reliable services with Java, Spring Boot and PostgreSQL. Join a small product team and learn through mentorship.",
              "recruiter",
              List.of("CSE", "IT"),
              2027,
              new BigDecimal("7.00"),
              0,
              new BigDecimal("9.50"),
              now.plus(14, ChronoUnit.DAYS)
            )
          )
        );
        drives.save(
          from(
            new CreateDrive(
              "Northstar Systems",
              "Associate Platform Engineer",
              "Pune · On-site",
              "Work on cloud infrastructure, APIs and automation. Strong programming fundamentals and curiosity are welcome.",
              "recruiter",
              List.of("CSE", "IT", "ECE"),
              2027,
              new BigDecimal("6.00"),
              1,
              new BigDecimal("7.20"),
              now.plus(21, ChronoUnit.DAYS)
            )
          )
        );
        drives.save(
          from(
            new CreateDrive(
              "Orbit Digital",
              "Backend Engineering Intern",
              "Remote",
              "A six-month internship focused on backend APIs, testing and service observability.",
              "recruiter",
              List.of("CSE", "IT", "ECE", "EE"),
              2027,
              new BigDecimal("8.00"),
              0,
              new BigDecimal("6.00"),
              now.plus(10, ChronoUnit.DAYS)
            )
          )
        );
      }
    };
  }
}
