package com.portfolio.timetable.service;

import com.portfolio.timetable.dto.RoomDtos.Request;
import com.portfolio.timetable.dto.RoomDtos.Response;
import com.portfolio.timetable.exception.ResourceNotFoundException;
import com.portfolio.timetable.model.Room;
import com.portfolio.timetable.repository.RoomRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @PreAuthorize("hasRole('COORDINATOR')")
    public Response create(Request request) {
        if (roomRepository.existsByRoomNumberIgnoreCase(request.roomNumber())) {
            throw new IllegalArgumentException("A room numbered '" + request.roomNumber() + "' already exists");
        }
        Room saved = roomRepository.save(new Room(request.roomNumber(), request.building(), request.capacity()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<Response> findAll() {
        return roomRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Response findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @PreAuthorize("hasRole('COORDINATOR')")
    public Response update(Long id, Request request) {
        Room room = getOrThrow(id);
        room.setRoomNumber(request.roomNumber());
        room.setBuilding(request.building());
        room.setCapacity(request.capacity());
        return toResponse(room);
    }

    @PreAuthorize("hasRole('COORDINATOR')")
    public void delete(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new ResourceNotFoundException("Room " + id + " not found");
        }
        roomRepository.deleteById(id);
    }

    Room getOrThrow(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room " + id + " not found"));
    }

    private Response toResponse(Room room) {
        return new Response(room.getId(), room.getRoomNumber(), room.getBuilding(), room.getCapacity());
    }
}
