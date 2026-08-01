package com.portfolio.timetable.web;

import com.portfolio.timetable.dto.CourseDtos.Request;
import com.portfolio.timetable.exception.ResourceNotFoundException;
import com.portfolio.timetable.service.CourseService;
import com.portfolio.timetable.service.DepartmentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/courses")
public class CourseWebController {

    private final CourseService courseService;
    private final DepartmentService departmentService;

    public CourseWebController(CourseService courseService, DepartmentService departmentService) {
        this.courseService = courseService;
        this.departmentService = departmentService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("courses", courseService.findAll());
        model.addAttribute("departments", departmentService.findAll());
        return "courses";
    }

    @PostMapping
    public String create(@RequestParam String code, @RequestParam String title,
                          @RequestParam int creditHours, @RequestParam Long departmentId,
                          RedirectAttributes redirectAttributes) {
        try {
            courseService.create(new Request(code, title, creditHours, departmentId));
            redirectAttributes.addFlashAttribute("successMessage", "Course added.");
        } catch (IllegalArgumentException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/courses";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            courseService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Course deleted.");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/courses";
    }
}
