package com.portfolio.timetable.service;

import com.portfolio.timetable.dto.DepartmentDtos.Request;
import com.portfolio.timetable.dto.DepartmentDtos.Response;
import com.portfolio.timetable.exception.ResourceNotFoundException;
import com.portfolio.timetable.model.Department;
import com.portfolio.timetable.repository.DepartmentRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @PreAuthorize("hasRole('COORDINATOR')")
    public Response create(Request request) {
        if (departmentRepository.existsByCodeIgnoreCase(request.code())) {
            throw new IllegalArgumentException("A department with code '" + request.code() + "' already exists");
        }
        Department saved = departmentRepository.save(new Department(request.code(), request.name()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<Response> findAll() {
        return departmentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Response findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @PreAuthorize("hasRole('COORDINATOR')")
    public Response update(Long id, Request request) {
        Department department = getOrThrow(id);
        department.setCode(request.code());
        department.setName(request.name());
        return toResponse(department);
    }

    @PreAuthorize("hasRole('COORDINATOR')")
    public void delete(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Department " + id + " not found");
        }
        departmentRepository.deleteById(id);
    }

    /** Package-private so other services can resolve a Department entity when building related entities. */
    Department getOrThrow(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department " + id + " not found"));
    }

    private Response toResponse(Department department) {
        return new Response(department.getId(), department.getCode(), department.getName());
    }
}
