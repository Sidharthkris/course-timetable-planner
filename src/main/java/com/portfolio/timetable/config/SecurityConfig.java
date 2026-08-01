package com.portfolio.timetable.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Two roles: COORDINATOR (full read/write access, mirrors the
 * project's real-world academic coordinator) and INSTRUCTOR
 * (view-only). The actual role check lives at the service layer via
 * {@code @PreAuthorize("hasRole('COORDINATOR')")} on every
 * create/update/delete method (see {@code service} package) — that's
 * the enforcement that matters, applying identically whether the
 * request came through the REST API or the Thymeleaf GUI. The rules
 * here just gate "must be logged in at all" and wire up how login
 * itself works.
 *
 * <p>Both session-based form login (for the browser GUI) and HTTP
 * Basic (for curl/Postman/Swagger "Try it out") are enabled on the
 * same filter chain, which is a common and supported combination.
 *
 * <p>Users are hardcoded in-memory for this portfolio project rather
 * than backed by a database — see the README for how a real
 * deployment would replace this with a persisted {@code User} entity.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails coordinator = User.withUsername("coordinator")
                .password(passwordEncoder.encode("coordinator123"))
                .roles("COORDINATOR")
                .build();

        UserDetails instructor = User.withUsername("instructor")
                .password(passwordEncoder.encode("instructor123"))
                .roles("INSTRUCTOR")
                .build();

        return new InMemoryUserDetailsManager(coordinator, instructor);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/access-denied", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/schedule", true)
                        .permitAll())
                .httpBasic(Customizer.withDefaults())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedPage("/access-denied"))
                // H2 console renders in a frame; only relevant on the 'dev' profile.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}
