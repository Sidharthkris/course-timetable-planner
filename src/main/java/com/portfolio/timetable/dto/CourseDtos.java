package com.portfolio.timetable.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CourseDtos {

    private CourseDtos() {
    }

    public record Request(
            @NotBlank(message = "Course code is required") String code,
            @NotBlank(message = "Course title is required") String title,
            @Positive(message = "Credit hours must be positive") int creditHours,
            @NotNull(message = "departmentId is required") Long departmentId) {
    }

    public record Response(Long id, String code, String title, int creditHours, DepartmentDtos.Response department) {
    }
}
