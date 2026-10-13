package dev.campus.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.campus.common.Contracts.*;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {
    "spring.datasource.url=jdbc:h2:mem:workflow;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "campus.outbox-enabled=false",
  }
)
class WorkflowIntegrationTest {

  @Value("${local.server.port}")
  int port;

  @Autowired
  ApplicationRepository applications;

  @Autowired
  HistoryRepository history;

  @Autowired
  OutboxRepository outbox;

  @Autowired
  ObjectMapper json;

  @MockitoBean
  ServiceClients clients;

  private final HttpClient http = HttpClient.newHttpClient();

  @BeforeEach
  void prepare() {
    outbox.deleteAll();
    history.deleteAll();
    applications.deleteAll();
    when(clients.student("student")).thenReturn(
      new StudentSnapshot(
        "student",
        "Aarav",
        "CSE",
        2027,
        new BigDecimal("8.4"),
        0,
        true
      )
    );
    when(clients.drive("drive")).thenReturn(
      new DriveSnapshot(
        "drive",
        "Aster",
        "Engineer",
        "recruiter",
        List.of("CSE"),
        2027,
        new BigDecimal("7"),
        0,
        Instant.now().plusSeconds(3600),
        "PUBLISHED"
      )
    );
  }

  HttpRequest request(String user, String method, String route, String body) {
    return HttpRequest.newBuilder(
      URI.create("http://127.0.0.1:" + port + route)
    )
      .header(
        "Authorization",
        "Basic " +
          Base64.getEncoder().encodeToString(
            (user + ":placement-demo").getBytes(StandardCharsets.UTF_8)
          )
      )
      .header("Content-Type", "application/json")
      .method(
        method,
        body == null
          ? HttpRequest.BodyPublishers.noBody()
          : HttpRequest.BodyPublishers.ofString(body)
      )
      .build();
  }

  HttpResponse<String> call(
    String user,
    String method,
    String route,
    String body
  ) throws Exception {
    return http.send(
      request(user, method, route, body),
      HttpResponse.BodyHandlers.ofString()
    );
  }

  String submit() throws Exception {
    var r = call(
      "student",
      "POST",
      "/api/applications",
      "{\"driveId\":\"drive\"}"
    );
    assertEquals(201, r.statusCode(), r.body());
    return json.readTree(r.body()).get("id").asText();
  }

  @Test
  void savesApplicationHistoryAndEventAtomically() throws Exception {
    submit();
    assertEquals(1, applications.count());
    assertEquals(1, history.count());
    assertEquals(1, outbox.count());
    assertTrue(
      applications
        .findAll()
        .getFirst()
        .eligibilitySnapshot.contains("minCgpa=7")
    );
  }

  @Test
  void simultaneousSubmissionsProduceOnlyOneApplication() throws Exception {
    var req = request(
      "student",
      "POST",
      "/api/applications",
      "{\"driveId\":\"drive\"}"
    );
    var a = http.sendAsync(req, HttpResponse.BodyHandlers.ofString());
    var b = http.sendAsync(req, HttpResponse.BodyHandlers.ofString());
    var codes = List.of(
      a.get(20, TimeUnit.SECONDS).statusCode(),
      b.get(20, TimeUnit.SECONDS).statusCode()
    );
    assertTrue(codes.contains(201), codes.toString());
    assertTrue(codes.contains(409), codes.toString());
    assertEquals(1, applications.count());
    assertEquals(1, outbox.count());
    assertEquals(1, history.count());
  }

  @Test
  void ineligibleApplicationHasNoSideEffects() throws Exception {
    when(clients.student("student")).thenReturn(
      new StudentSnapshot(
        "student",
        "Aarav",
        "CSE",
        2027,
        new BigDecimal("6"),
        0,
        true
      )
    );
    var r = call(
      "student",
      "POST",
      "/api/applications",
      "{\"driveId\":\"drive\"}"
    );
    assertEquals(422, r.statusCode());
    assertTrue(r.body().contains("Minimum CGPA"));
    assertEquals(0, applications.count());
    assertEquals(0, outbox.count());
  }

  @Test
  void unavailableDependencyDoesNotSave() throws Exception {
    when(clients.drive("drive")).thenThrow(
      new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unavailable")
    );
    assertEquals(
      503,
      call(
        "student",
        "POST",
        "/api/applications",
        "{\"driveId\":\"drive\"}"
      ).statusCode()
    );
    assertEquals(0, applications.count());
  }

  @Test
  void ownershipAndRolesAreEnforced() throws Exception {
    String id = submit();
    assertEquals(
      404,
      call(
        "student2",
        "GET",
        "/api/applications/" + id + "/history",
        null
      ).statusCode()
    );
    assertEquals(
      403,
      call("student", "GET", "/api/reports", null).statusCode()
    );
    assertEquals(
      403,
      call(
        "officer",
        "POST",
        "/api/applications",
        "{\"driveId\":\"drive\"}"
      ).statusCode()
    );
    assertEquals(
      403,
      call(
        "student",
        "PATCH",
        "/api/applications/" + id + "/status",
        "{\"status\":\"SHORTLISTED\"}"
      ).statusCode()
    );
  }

  @Test
  void fullWorkflowPreservesAuditAndTerminalState() throws Exception {
    String id = submit();
    String route = "/api/applications/" + id + "/status";
    assertEquals(
      409,
      call("officer", "PATCH", route, "{\"status\":\"SELECTED\"}").statusCode()
    );
    assertEquals(
      200,
      call(
        "recruiter",
        "PATCH",
        route,
        "{\"status\":\"SHORTLISTED\",\"note\":\"Technical screening passed\"}"
      ).statusCode()
    );
    assertEquals(
      400,
      call(
        "officer",
        "PATCH",
        route,
        "{\"status\":\"INTERVIEW_SCHEDULED\"}"
      ).statusCode()
    );
    for (String invalidUrl : List.of(
      "example.com/interview",
      "ftp://example.com/interview",
      "javascript:alert(1)"
    )) {
      var invalid = call(
        "officer",
        "PATCH",
        route,
        json.writeValueAsString(
          Map.of(
            "status",
            "INTERVIEW_SCHEDULED",
            "interviewAt",
            Instant.now().plusSeconds(7200).toString(),
            "meetingUrl",
            invalidUrl
          )
        )
      );
      assertEquals(400, invalid.statusCode(), invalid.body());
    }
    assertEquals(2, history.count());
    assertEquals(2, outbox.count());
    assertEquals(
      200,
      call(
        "officer",
        "PATCH",
        route,
        "{\"status\":\"INTERVIEW_SCHEDULED\",\"interviewAt\":\"" +
          Instant.now().plusSeconds(7200) +
          "\",\"meetingUrl\":\"https://example.com/interview\"}"
      ).statusCode()
    );
    assertEquals(
      200,
      call("officer", "PATCH", route, "{\"status\":\"SELECTED\"}").statusCode()
    );
    assertEquals(
      409,
      call("student", "PATCH", route, "{\"status\":\"WITHDRAWN\"}").statusCode()
    );
    assertEquals(4, history.count());
    assertEquals(4, outbox.count());
  }
}
