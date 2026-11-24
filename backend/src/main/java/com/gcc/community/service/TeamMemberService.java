package com.gcc.community.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gcc.community.common.ApiException;
import com.gcc.community.domain.TeamMember;
import com.gcc.community.dto.TeamMemberRequest;
import com.gcc.community.dto.TeamMemberView;
import com.gcc.community.repository.TeamMemberRepository;
import com.gcc.community.validation.UrlRules;

/** 制作组成员的业务。 */
@Service
public class TeamMemberService {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private TeamMemberRepository teamMemberRepository;

    public List<TeamMemberView> list(long projectId) {
        projectService.requireOwnedProject(projectId);
        return teamMemberRepository.findByProjectIdOrdered(projectId).stream().map(this::toView).toList();
    }

    @Transactional
    public TeamMemberView create(long projectId, TeamMemberRequest request) {
        projectService.requireOwnedProject(projectId);
        Map<String, String> errors = new LinkedHashMap<>();
        String name = request.name();
        String avatarUrl = request.avatarUrl() == null ? "" : request.avatarUrl();
        String role = request.role() == null ? "" : request.role();
        String bio = request.bio() == null ? "" : request.bio();
        int sortOrder = request.sortOrder() == null ? 0 : request.sortOrder();

        ContentValidation.requiredText(errors, "name", name, 60);
        validateAvatar(errors, avatarUrl);
        ContentValidation.optionalText(errors, "role", role, 60);
        ContentValidation.optionalText(errors, "bio", bio, 500);
        ContentValidation.requirePositiveOrZero(errors, "sortOrder", request.sortOrder());
        ContentValidation.throwIfAny("制作组成员参数校验失败", errors);

        long id = teamMemberRepository.insert(projectId, name, avatarUrl, role, bio, sortOrder);
        return toView(new TeamMember(id, projectId, name, avatarUrl, role, bio, sortOrder));
    }

    @Transactional
    public TeamMemberView update(long memberId, TeamMemberRequest request) {
        TeamMember existing = requireMember(memberId);
        projectService.requireOwnedProject(existing.projectId());
        Map<String, String> errors = new LinkedHashMap<>();
        String name = request.name() == null ? existing.name() : request.name();
        String avatarUrl = request.avatarUrl() == null ? existing.avatarUrl() : request.avatarUrl();
        String role = request.role() == null ? existing.role() : request.role();
        String bio = request.bio() == null ? existing.bio() : request.bio();
        int sortOrder = request.sortOrder() == null ? existing.sortOrder() : request.sortOrder();

        ContentValidation.requiredText(errors, "name", name, 60);
        validateAvatar(errors, avatarUrl);
        ContentValidation.optionalText(errors, "role", role, 60);
        ContentValidation.optionalText(errors, "bio", bio, 500);
        ContentValidation.requirePositiveOrZero(errors, "sortOrder", request.sortOrder());
        ContentValidation.throwIfAny("制作组成员参数校验失败", errors);

        teamMemberRepository.update(memberId, name, avatarUrl, role, bio, sortOrder);
        return toView(new TeamMember(memberId, existing.projectId(), name, avatarUrl, role, bio, sortOrder));
    }

    @Transactional
    public void delete(long memberId) {
        TeamMember existing = requireMember(memberId);
        projectService.requireOwnedProject(existing.projectId());
        teamMemberRepository.deleteById(memberId);
    }

    // 公开端返回全部成员
    public List<TeamMemberView> publicList(long projectId) {
        return teamMemberRepository.findByProjectIdOrdered(projectId).stream().map(this::toView).toList();
    }

    private TeamMember requireMember(long memberId) {
        return teamMemberRepository.findById(memberId)
                .orElseThrow(() -> ApiException.notFound("制作组成员不存在"));
    }

    private void validateAvatar(Map<String, String> errors, String avatarUrl) {
        ContentValidation.optionalText(errors, "avatarUrl", avatarUrl, 300);
        if (avatarUrl != null && !avatarUrl.isBlank() && !UrlRules.isValidHttpUrl(avatarUrl)) {
            errors.put("avatarUrl", "需要为空或 http(s) 绝对 URL");
        }
    }

    private TeamMemberView toView(TeamMember member) {
        return new TeamMemberView(member.id(), member.name(), member.avatarUrl(), member.role(),
                member.bio(), member.sortOrder());
    }
}
