package dev.campus.notification;

import org.springframework.amqp.core.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

@Configuration
@ConditionalOnProperty(name = "campus.messaging", havingValue = "rabbit")
public class RabbitConfiguration {

  @Bean
  TopicExchange events() {
    return new TopicExchange("campus.events", true, false);
  }

  @Bean
  DirectExchange deadLetters() {
    return new DirectExchange("campus.dlx", true, false);
  }

  @Bean
  Queue notificationsQueue() {
    return QueueBuilder.durable("campus.notifications")
      .deadLetterExchange("campus.dlx")
      .deadLetterRoutingKey("failed")
      .build();
  }

  @Bean
  Queue failedQueue() {
    return QueueBuilder.durable("campus.notifications.failed").build();
  }

  @Bean
  Binding eventBinding() {
    return BindingBuilder.bind(notificationsQueue())
      .to(events())
      .with("application.changed");
  }

  @Bean
  Binding failedBinding() {
    return BindingBuilder.bind(failedQueue()).to(deadLetters()).with("failed");
  }
}
