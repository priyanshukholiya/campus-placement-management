package dev.campus.gateway;

import dev.campus.common.ApiErrors;
import dev.campus.common.SecurityConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({
  SecurityConfiguration.class,
  ApiErrors.class,
  dev.campus.common.SessionController.class,
})
public class ServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ServiceApplication.class, args);
  }
}
