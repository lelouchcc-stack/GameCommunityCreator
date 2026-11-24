package com.gcc.community.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gcc.community.common.ApiResponse;
import com.gcc.community.dto.CreateProjectRequest;
import com.gcc.community.dto.ProjectView;
import com.gcc.community.dto.UpdateProjectRequest;
import com.gcc.community.service.ProjectService;

import jakarta.validation.Valid;

/** 社区项目的增删改查接口。 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    // 项目列表，按更新时间倒序
    @GetMapping
    public ApiResponse<List<ProjectView>> list() {
        return ApiResponse.ok(projectService.list());
    }

    // 新建项目，默认页面和游戏信息一起建好
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectView>> create(@Valid @RequestBody CreateProjectRequest request) {
        ProjectView view = projectService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(view));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectView> get(@PathVariable long id) {
        return ApiResponse.ok(projectService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProjectView> update(@PathVariable long id,
            @Valid @RequestBody UpdateProjectRequest request) {
        return ApiResponse.ok(projectService.update(id, request));
    }

    // 删除项目，关联的数据一起清掉
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
