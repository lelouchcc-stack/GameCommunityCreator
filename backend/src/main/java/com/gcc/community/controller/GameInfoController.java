package com.gcc.community.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gcc.community.common.ApiResponse;
import com.gcc.community.domain.GameInfo;
import com.gcc.community.dto.UpdateGameInfoRequest;
import com.gcc.community.service.GameInfoService;

import jakarta.validation.Valid;

/** 游戏信息的读写接口。 */
@RestController
@RequestMapping("/api/projects/{projectId}/game-info")
public class GameInfoController {

    @Autowired
    private GameInfoService gameInfoService;

    @GetMapping
    public ApiResponse<GameInfo> get(@PathVariable long projectId) {
        return ApiResponse.ok(gameInfoService.get(projectId));
    }

    @PutMapping
    public ApiResponse<GameInfo> update(@PathVariable long projectId,
            @Valid @RequestBody UpdateGameInfoRequest request) {
        return ApiResponse.ok(gameInfoService.update(projectId, request));
    }
}
