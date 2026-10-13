package dev.campus.notification;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = "spring.datasource.url=jdbc:h2:mem:notifications;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
)
class NotificationIntegrationTest {

  @Value("${local.server.port}")
  int port;

  @Autowired
  NotificationRepository repo;

  HttpResponse<String> call(
    String user,
    String password,
    String method,
    String path,
    String body
  ) throws Exception {
    return HttpClient.newHttpClient().send(
      HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
        .header(
          "Authorization",
          "Basic " +
            Base64.getEncoder().encodeToString(
              (user + ":" + password).getBytes(StandardCharsets.UTF_8)
            )
        )
        .header("Content-Type", "application/json")
        .method(
          method,
          body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(body)
        )
        .build(),
      HttpResponse.BodyHandlers.ofString()
    );
  }

  @Test
  void duplicateDeliveryIsIdempotentAndInboxIsPrivate() throws Exception {
    var event =
      "{\"eventId\":\"duplicate-event\",\"recipient\":\"student\",\"title\":\"Submitted\",\"message\":\"Application accepted\",\"occurredAt\":\"2026-10-13T00:00:00Z\"}";
    assertEquals(
      403,
      call(
        "student",
        "placement-demo",
        "POST",
        "/internal/notifications",
        event
      ).statusCode()
    );
    assertEquals(
      204,
      call(
        "service",
        "local-service-demo",
        "POST",
        "/internal/notifications",
        event
      ).statusCode()
    );
    assertEquals(
      204,
      call(
        "service",
        "local-service-demo",
        "POST",
        "/internal/notifications",
        event
      ).statusCode()
    );
    assertEquals(1, repo.count());
    assertEquals(
      "[]",
      call(
        "student2",
        "placement-demo",
        "GET",
        "/api/notifications",
        null
      ).body()
    );
    assertEquals(
      404,
      call(
        "student2",
        "placement-demo",
        "PATCH",
        "/api/notifications/duplicate-event/read",
        null
      ).statusCode()
    );
    assertEquals(
      200,
      call(
        "student",
        "placement-demo",
        "PATCH",
        "/api/notifications/duplicate-event/read",
        null
      ).statusCode()
    );
  }
}
