package com.portfolio.timetable.service;

import com.portfolio.timetable.dto.CourseDtos;
import com.portfolio.timetable.dto.DepartmentDtos;
import com.portfolio.timetable.dto.InstructorDtos;
import com.portfolio.timetable.dto.RoomDtos;
import com.portfolio.timetable.dto.ScheduleEntryDtos.Request;
import com.portfolio.timetable.dto.ScheduleEntryDtos.Response;
import com.portfolio.timetable.exception.ResourceNotFoundException;
import com.portfolio.timetable.exception.ScheduleConflictException;
import com.portfolio.timetable.model.*;
import com.portfolio.timetable.repository.ScheduleEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;

@Service
@Transactional
public class ScheduleEntryService {

    private final ScheduleEntryRepository scheduleEntryRepository;
    private final CourseService courseService;
    private final InstructorService instructorService;
    private final RoomService roomService;
    private final ConflictDetectionService conflictDetectionService;

    public ScheduleEntryService(ScheduleEntryRepository scheduleEntryRepository,
                                 CourseService courseService,
                                 InstructorService instructorService,
                                 RoomService roomService,
                                 ConflictDetectionService conflictDetectionService) {
        this.scheduleEntryRepository = scheduleEntryRepository;
        this.courseService = courseService;
        this.instructorService = instructorService;
        this.roomService = roomService;
        this.conflictDetectionService = conflictDetectionService;
    }

    public Response create(Request request) {
        ScheduleEntry candidate = buildCandidate(request);
        rejectIfConflicting(candidate, null);
        return toResponse(scheduleEntryRepository.save(candidate));
    }

    public Response update(Long id, Request request) {
        ScheduleEntry existing = getOrThrow(id);
        ScheduleEntry candidate = buildCandidate(request);
        rejectIfConflicting(candidate, id);

        existing.setCourse(candidate.getCourse());
        existing.setInstructor(candidate.getInstructor());
        existing.setRoom(candidate.getRoom());
        existing.setDayOfWeek(candidate.getDayOfWeek());
        existing.setStartTime(candidate.getStartTime());
        existing.setEndTime(candidate.getEndTime());
        return toResponse(existing);
    }

    @Transactional(readOnly = true)
    public Response findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<Response> search(Long instructorId, Long roomId, Long courseId, DayOfWeek dayOfWeek, Pageable pageable) {
        return scheduleEntryRepository.search(instructorId, roomId, courseId, dayOfWeek, pageable)
                .map(this::toResponse);
    }

    public void delete(Long id) {
        if (!scheduleEntryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Schedule entry " + id + " not found");
        }
        scheduleEntryRepository.deleteById(id);
    }

    /**
     * Checks whether a proposed entry would conflict, without saving
     * anything — lets a client validate a slot before committing to it.
     */
    @Transactional(readOnly = true)
    public List<Response> checkConflicts(Request request) {
        ScheduleEntry candidate = buildCandidate(request);
        return conflictDetectionService.findConflicts(candidate, null).stream().map(this::toResponse).toList();
    }

    private void rejectIfConflicting(ScheduleEntry candidate, Long excludeEntryId) {
        List<ScheduleEntry> conflicts = conflictDetectionService.findConflicts(candidate, excludeEntryId);
        if (!conflicts.isEmpty()) {
            String message = "This slot conflicts with %d existing entry/entries for the same instructor or room"
                    .formatted(conflicts.size());
            throw new ScheduleConflictException(message, conflicts);
        }
    }

    private ScheduleEntry buildCandidate(Request request) {
        if (!request.startTime().isBefore(request.endTime())) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
        Course course = courseService.getOrThrow(request.courseId());
        Instructor instructor = instructorService.getOrThrow(request.instructorId());
        Room room = roomService.getOrThrow(request.roomId());
        return new ScheduleEntry(course, instructor, room, request.dayOfWeek(), request.startTime(), request.endTime());
    }

    private ScheduleEntry getOrThrow(Long id) {
        return scheduleEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule entry " + id + " not found"));
    }

    public Response toResponse(ScheduleEntry entry) {
        Department courseDept = entry.getCourse().getDepartment();
        DepartmentDtos.Response courseDeptResponse = courseDept == null ? null
                : new DepartmentDtos.Response(courseDept.getId(), courseDept.getCode(), courseDept.getName());
        CourseDtos.Response courseResponse = new CourseDtos.Response(
                entry.getCourse().getId(), entry.getCourse().getCode(), entry.getCourse().getTitle(),
                entry.getCourse().getCreditHours(), courseDeptResponse);

        Department instructorDept = entry.getInstructor().getDepartment();
        DepartmentDtos.Response instructorDeptResponse = instructorDept == null ? null
                : new DepartmentDtos.Response(instructorDept.getId(), instructorDept.getCode(), instructorDept.getName());
        InstructorDtos.Response instructorResponse = new InstructorDtos.Response(
                entry.getInstructor().getId(), entry.getInstructor().getFullName(),
                entry.getInstructor().getEmail(), instructorDeptResponse);

        RoomDtos.Response roomResponse = new RoomDtos.Response(
                entry.getRoom().getId(), entry.getRoom().getRoomNumber(),
                entry.getRoom().getBuilding(), entry.getRoom().getCapacity());

        return new Response(entry.getId(), courseResponse, instructorResponse, roomResponse,
                entry.getDayOfWeek(), entry.getStartTime(), entry.getEndTime());
    }
}
