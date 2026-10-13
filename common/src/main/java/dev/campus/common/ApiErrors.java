package dev.campus.common;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors {

  public record ErrorBody(String code, String message, List<String> details) {}

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<ErrorBody> response(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode()).body(
      new ErrorBody(
        "REQUEST_REJECTED",
        e.getReason() == null ? "Request rejected." : e.getReason(),
        List.of()
      )
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorBody> validation(MethodArgumentNotValidException e) {
    return ResponseEntity.badRequest().body(
      new ErrorBody(
        "VALIDATION_FAILED",
        "Check the highlighted information.",
        e
          .getBindingResult()
          .getFieldErrors()
          .stream()
          .map(f -> f.getField() + ": " + f.getDefaultMessage())
          .toList()
      )
    );
  }
}
