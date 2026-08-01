package com.portfolio.timetable.web;

import com.portfolio.timetable.dto.DepartmentDtos.Request;
import com.portfolio.timetable.exception.ResourceNotFoundException;
import com.portfolio.timetable.service.DepartmentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/departments")
public class DepartmentWebController {

    private final DepartmentService departmentService;

    public DepartmentWebController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("departments", departmentService.findAll());
        return "departments";
    }

    @PostMapping
    public String create(@RequestParam String code, @RequestParam String name,
                          RedirectAttributes redirectAttributes) {
        try {
            departmentService.create(new Request(code, name));
            redirectAttributes.addFlashAttribute("successMessage", "Department created.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/departments";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            departmentService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Department deleted.");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/departments";
    }
}
