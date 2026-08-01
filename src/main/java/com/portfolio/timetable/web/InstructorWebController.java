package com.portfolio.timetable.web;

import com.portfolio.timetable.dto.InstructorDtos.Request;
import com.portfolio.timetable.exception.ResourceNotFoundException;
import com.portfolio.timetable.service.DepartmentService;
import com.portfolio.timetable.service.InstructorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/instructors")
public class InstructorWebController {

    private final InstructorService instructorService;
    private final DepartmentService departmentService;

    public InstructorWebController(InstructorService instructorService, DepartmentService departmentService) {
        this.instructorService = instructorService;
        this.departmentService = departmentService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("instructors", instructorService.findAll());
        model.addAttribute("departments", departmentService.findAll());
        return "instructors";
    }

    @PostMapping
    public String create(@RequestParam String fullName, @RequestParam(required = false) String email,
                          @RequestParam Long departmentId, RedirectAttributes redirectAttributes) {
        try {
            instructorService.create(new Request(fullName, email, departmentId));
            redirectAttributes.addFlashAttribute("successMessage", "Instructor added.");
        } catch (IllegalArgumentException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/instructors";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            instructorService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Instructor deleted.");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/instructors";
    }
}
