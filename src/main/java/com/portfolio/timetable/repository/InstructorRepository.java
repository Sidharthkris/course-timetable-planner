package com.portfolio.timetable.repository;

import com.portfolio.timetable.model.Instructor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstructorRepository extends JpaRepository<Instructor, Long> {
    boolean existsByEmailIgnoreCase(String email);
}
