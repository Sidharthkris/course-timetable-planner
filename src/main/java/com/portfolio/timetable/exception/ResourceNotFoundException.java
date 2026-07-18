package com.portfolio.timetable.exception;

/** Thrown when a requested entity (department, instructor, room, course, or schedule entry) doesn't exist. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
