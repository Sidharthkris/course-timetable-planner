package com.portfolio.timetable.service;

import com.portfolio.timetable.dto.DepartmentDtos;
import com.portfolio.timetable.dto.InstructorDtos.Request;
import com.portfolio.timetable.dto.InstructorDtos.Response;
import com.portfolio.timetable.exception.ResourceNotFoundException;
import com.portfolio.timetable.model.Department;
import com.portfolio.timetable.model.Instructor;
import com.portfolio.timetable.repository.InstructorRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class InstructorService {

    private final InstructorRepository instructorRepository;
    private final DepartmentService departmentService;

    public InstructorService(InstructorRepository instructorRepository, DepartmentService departmentService) {
        this.instructorRepository = instructorRepository;
        this.departmentService = departmentService;
    }

    @PreAuthorize("hasRole('COORDINATOR')")
    public Response create(Request request) {
        Department department = departmentService.getOrThrow(request.departmentId());
        Instructor saved = instructorRepository.save(
                new Instructor(request.fullName(), request.email(), department));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<Response> findAll() {
        return instructorRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Response findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @PreAuthorize("hasRole('COORDINATOR')")
    public Response update(Long id, Request request) {
        Instructor instructor = getOrThrow(id);
        instructor.setFullName(request.fullName());
        instructor.setEmail(request.email());
        instructor.setDepartment(departmentService.getOrThrow(request.departmentId()));
        return toResponse(instructor);
    }

    @PreAuthorize("hasRole('COORDINATOR')")
    public void delete(Long id) {
        if (!instructorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Instructor " + id + " not found");
        }
        instructorRepository.deleteById(id);
    }

    Instructor getOrThrow(Long id) {
        return instructorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instructor " + id + " not found"));
    }

    private Response toResponse(Instructor instructor) {
        Department department = instructor.getDepartment();
        DepartmentDtos.Response departmentResponse = department == null ? null
                : new DepartmentDtos.Response(department.getId(), department.getCode(), department.getName());
        return new Response(instructor.getId(), instructor.getFullName(), instructor.getEmail(), departmentResponse);
    }
}
