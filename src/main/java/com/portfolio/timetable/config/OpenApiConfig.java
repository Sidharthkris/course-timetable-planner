package com.portfolio.timetable.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BASIC_AUTH = "basicAuth";

    @Bean
    public OpenAPI timetableOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Course Timetable Planner API")
                        .description("Manages courses, instructors, rooms, and the schedule entries "
                                + "that tie them together, with automatic conflict detection so no "
                                + "instructor or room is ever double-booked. Writes require the "
                                + "COORDINATOR role — use the demo credentials in the README with "
                                + "Swagger's Authorize button (HTTP Basic).")
                        .version("v1.0.0"))
                .components(new Components().addSecuritySchemes(BASIC_AUTH,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH));
    }
}
