package com.portfolio.timetable.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI timetableOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Course Timetable Planner API")
                .description("Manages courses, instructors, rooms, and the schedule entries "
                        + "that tie them together, with automatic conflict detection so no "
                        + "instructor or room is ever double-booked.")
                .version("v1.0.0"));
    }
}
