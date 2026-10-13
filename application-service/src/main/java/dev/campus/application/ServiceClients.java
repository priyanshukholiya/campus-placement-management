package dev.campus.application;

import dev.campus.common.Contracts.*;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ServiceClients {

  private final RestClient students, recruitment;

  public ServiceClients(
    @Value("${campus.students-url}") String studentsUrl,
    @Value("${campus.recruitment-url}") String recruitmentUrl,
    @Value("${campus.service-password}") String password
  ) {
    students = client(studentsUrl, password);
    recruitment = client(recruitmentUrl, password);
  }

  static RestClient client(String url, String password) {
    var factory = new JdkClientHttpRequestFactory(
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()
    );
    factory.setReadTimeout(Duration.ofSeconds(4));
    return RestClient.builder()
      .baseUrl(url)
      .requestFactory(factory)
      .defaultHeaders(h -> h.setBasicAuth("service", password))
      .build();
  }

  public StudentSnapshot student(String id) {
    return fetch(
      students,
      "/internal/students/{id}",
      id,
      StudentSnapshot.class
    );
  }

  public DriveSnapshot drive(String id) {
    return fetch(recruitment, "/internal/drives/{id}", id, DriveSnapshot.class);
  }

  private <T> T fetch(
    RestClient client,
    String path,
    String id,
    Class<T> type
  ) {
    try {
      var result = client.get().uri(path, id).retrieve().body(type);
      if (result == null) throw new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE,
        "A required service returned an empty response. Please retry."
      );
      return result;
    } catch (HttpClientErrorException.NotFound e) {
      throw new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "Student or drive not found."
      );
    } catch (RestClientException e) {
      throw new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE,
        "A required service is unavailable. No unchecked application was saved; please retry."
      );
    }
  }
}
