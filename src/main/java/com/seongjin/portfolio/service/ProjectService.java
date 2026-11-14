package com.seongjin.portfolio.service;

import com.seongjin.portfolio.entity.Project;
import com.seongjin.portfolio.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;

    public List<Project> getAllOrdered() {
        return projectRepository.findAllByOrderByDisplayOrderAsc();
    }

    /**
     * displayOrder 순서를 유지하면서 category별로 묶어서 반환한다.
     * (LinkedHashMap이라 처음 등장한 category 순서 그대로 유지됨)
     */
    public Map<String, List<Project>> getAllGroupedByCategory() {
        return getAllOrdered().stream()
                .collect(Collectors.groupingBy(
                        Project::getCategory,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    public Project getById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로젝트입니다. id=" + id));
    }

}
