package com.seongjin.portfolio.controller;

import com.seongjin.portfolio.entity.Project;
import com.seongjin.portfolio.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class PortfolioController {

    private final ProjectService projectService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("groupedProjects", projectService.getAllGroupedByCategory());
        return "index";
    }

    @GetMapping("/projects/{id}")
    public String projectDetail(@PathVariable Long id, Model model) {
        model.addAttribute("project", projectService.getById(id));
        return "project-detail";
    }

}
