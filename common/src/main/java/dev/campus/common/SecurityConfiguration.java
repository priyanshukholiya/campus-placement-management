package dev.campus.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

  @Bean
  UserDetailsService users(
    @Value("${campus.demo-password}") String demo,
    @Value("${campus.service-password}") String internal
  ) {
    var encoder = new BCryptPasswordEncoder();
    String hash = encoder.encode(demo);
    return new InMemoryUserDetailsManager(
      User.withUsername("student").password(hash).roles("STUDENT").build(),
      User.withUsername("student2").password(hash).roles("STUDENT").build(),
      User.withUsername("officer").password(hash).roles("OFFICER").build(),
      User.withUsername("recruiter").password(hash).roles("RECRUITER").build(),
      User.withUsername("service")
        .password(encoder.encode(internal))
        .roles("SERVICE")
        .build()
    );
  }

  @Bean
  BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    // Local demo: credentials are explicitly attached to API requests, never stored in cookies.
    return http
      .csrf(c -> c.disable())
      .sessionManagement(s ->
        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )
      .authorizeHttpRequests(a ->
        a
          .requestMatchers("/actuator/health")
          .permitAll()
          .requestMatchers("/internal/**")
          .hasRole("SERVICE")
          .anyRequest()
          .authenticated()
      )
      .httpBasic(b ->
        b.authenticationEntryPoint((req, res, e) -> {
          res.setStatus(401);
          res.setContentType("application/json");
          res
            .getWriter()
            .write(
              "{\"code\":\"UNAUTHORIZED\",\"message\":\"Sign in with a valid demo account.\"}"
            );
        })
      )
      .build();
  }
}
