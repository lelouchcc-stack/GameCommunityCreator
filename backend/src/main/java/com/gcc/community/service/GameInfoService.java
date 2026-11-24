package com.gcc.community.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gcc.community.common.ApiException;
import com.gcc.community.domain.GameInfo;
import com.gcc.community.dto.UpdateGameInfoRequest;
import com.gcc.community.repository.GameInfoRepository;

/** 游戏信息的业务。 */
@Service
public class GameInfoService {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private GameInfoRepository gameInfoRepository;

    public GameInfo get(long projectId) {
        projectService.requireOwnedProject(projectId);
        return gameInfoRepository.findByProjectId(projectId)
                .orElseThrow(() -> ApiException.notFound("游戏信息不存在"));
    }

    @Transactional
    public GameInfo update(long projectId, UpdateGameInfoRequest request) {
        projectService.requireOwnedProject(projectId);
        GameInfo info = new GameInfo(projectId,
                request.name(),
                normalize(request.tagline()),
                normalize(request.description()),
                normalize(request.coverUrl()),
                normalize(request.platforms()),
                normalize(request.releaseStatus()));
        if (gameInfoRepository.update(info) == 0) {
            // 正常创建项目时已经建过，没有就补一条
            gameInfoRepository.insert(info);
        }
        return info;
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }
}
