package com.portfolio.timetable.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class InstructorDtos {

    private InstructorDtos() {
    }

    public record Request(
            @NotBlank(message = "Full name is required") String fullName,
            @Email(message = "Email must be valid") String email,
            @NotNull(message = "departmentId is required") Long departmentId) {
    }

    public record Response(Long id, String fullName, String email, DepartmentDtos.Response department) {
    }
}
