package com.portfolio.timetable.repository;

import com.portfolio.timetable.model.ScheduleEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.util.List;

public interface ScheduleEntryRepository extends JpaRepository<ScheduleEntry, Long> {

    /** All entries for a given instructor on a given day — used for conflict checks. */
    List<ScheduleEntry> findByInstructorIdAndDayOfWeek(Long instructorId, DayOfWeek dayOfWeek);

    /** All entries for a given room on a given day — used for conflict checks. */
    List<ScheduleEntry> findByRoomIdAndDayOfWeek(Long roomId, DayOfWeek dayOfWeek);

    /**
     * Paginated listing with each filter optional (pass null to skip
     * it) — backs {@code GET /api/schedule-entries?instructorId=&roomId=&dayOfWeek=}.
     */
    @Query("""
            SELECT s FROM ScheduleEntry s
            WHERE (:instructorId IS NULL OR s.instructor.id = :instructorId)
              AND (:roomId IS NULL OR s.room.id = :roomId)
              AND (:courseId IS NULL OR s.course.id = :courseId)
              AND (:dayOfWeek IS NULL OR s.dayOfWeek = :dayOfWeek)
            """)
    Page<ScheduleEntry> search(@Param("instructorId") Long instructorId,
                                @Param("roomId") Long roomId,
                                @Param("courseId") Long courseId,
                                @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                Pageable pageable);
}
