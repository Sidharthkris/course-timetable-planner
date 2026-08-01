package com.portfolio.timetable.service;

import com.portfolio.timetable.dto.CourseDtos.Request;
import com.portfolio.timetable.dto.CourseDtos.Response;
import com.portfolio.timetable.dto.DepartmentDtos;
import com.portfolio.timetable.exception.ResourceNotFoundException;
import com.portfolio.timetable.model.Course;
import com.portfolio.timetable.model.Department;
import com.portfolio.timetable.repository.CourseRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final DepartmentService departmentService;

    public CourseService(CourseRepository courseRepository, DepartmentService departmentService) {
        this.courseRepository = courseRepository;
        this.departmentService = departmentService;
    }

    @PreAuthorize("hasRole('COORDINATOR')")
    public Response create(Request request) {
        if (courseRepository.existsByCodeIgnoreCase(request.code())) {
            throw new IllegalArgumentException("A course with code '" + request.code() + "' already exists");
        }
        Department department = departmentService.getOrThrow(request.departmentId());
        Course saved = courseRepository.save(
                new Course(request.code(), request.title(), request.creditHours(), department));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<Response> findAll() {
        return courseRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Response findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @PreAuthorize("hasRole('COORDINATOR')")
    public Response update(Long id, Request request) {
        Course course = getOrThrow(id);
        course.setCode(request.code());
        course.setTitle(request.title());
        course.setCreditHours(request.creditHours());
        course.setDepartment(departmentService.getOrThrow(request.departmentId()));
        return toResponse(course);
    }

    @PreAuthorize("hasRole('COORDINATOR')")
    public void delete(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Course " + id + " not found");
        }
        courseRepository.deleteById(id);
    }

    Course getOrThrow(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course " + id + " not found"));
    }

    private Response toResponse(Course course) {
        Department department = course.getDepartment();
        DepartmentDtos.Response departmentResponse = department == null ? null
                : new DepartmentDtos.Response(department.getId(), department.getCode(), department.getName());
        return new Response(course.getId(), course.getCode(), course.getTitle(), course.getCreditHours(), departmentResponse);
    }
}
