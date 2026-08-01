package com.portfolio.timetable.web;

import com.portfolio.timetable.dto.RoomDtos.Request;
import com.portfolio.timetable.exception.ResourceNotFoundException;
import com.portfolio.timetable.service.RoomService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/rooms")
public class RoomWebController {

    private final RoomService roomService;

    public RoomWebController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rooms", roomService.findAll());
        return "rooms";
    }

    @PostMapping
    public String create(@RequestParam String roomNumber, @RequestParam(required = false) String building,
                          @RequestParam int capacity, RedirectAttributes redirectAttributes) {
        try {
            roomService.create(new Request(roomNumber, building, capacity));
            redirectAttributes.addFlashAttribute("successMessage", "Room added.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/rooms";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            roomService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Room deleted.");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/rooms";
    }
}
