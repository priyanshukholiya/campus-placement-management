package dev.campus.common;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class SessionController {

  @GetMapping("/api/session")
  public Map<String, Object> session(Authentication auth) {
    return Map.of(
      "username",
      auth.getName(),
      "roles",
      auth
        .getAuthorities()
        .stream()
        .map(a -> a.getAuthority().replace("ROLE_", ""))
        .toList()
    );
  }
}
