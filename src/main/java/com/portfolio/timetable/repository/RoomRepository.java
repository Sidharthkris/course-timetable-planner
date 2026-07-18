package com.portfolio.timetable.repository;

import com.portfolio.timetable.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
    boolean existsByRoomNumberIgnoreCase(String roomNumber);
}
