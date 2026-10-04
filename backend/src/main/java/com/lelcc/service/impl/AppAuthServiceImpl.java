package com.lelcc.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lelcc.common.BusinessException;
import com.lelcc.common.UserContext;
import com.lelcc.common.UserType;
import com.lelcc.dto.MemberInfoVO;
import com.lelcc.dto.MemberLoginDTO;
import com.lelcc.dto.MemberLoginVO;
import com.lelcc.dto.MemberRegisterDTO;
import com.lelcc.entity.Member;
import com.lelcc.mapper.MemberMapper;
import com.lelcc.service.AppAuthService;
import com.lelcc.utils.JwtUtils;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 玩家认证。与 AdminAuthServiceImpl 同构：账号不存在与密码错误返回同一句提示（避免枚举账号），
 * 封禁账号在登录时就被拦下；注册即登录，省掉一次往返。
 */
@Service
public class AppAuthServiceImpl implements AppAuthService {

    private static final int STATUS_NORMAL = 1;

    @Resource
    private MemberMapper memberMapper;

    @Resource
    private JwtUtils jwtUtils;

    @Override
    public MemberLoginVO login(MemberLoginDTO dto) {
        Member member = findByUsername(dto.username());
        if (member == null || !BCrypt.checkpw(dto.password(), member.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        requireNormal(member);
        return issue(member);
    }

    @Override
    public MemberLoginVO register(MemberRegisterDTO dto) {
        if (findByUsername(dto.username()) != null) {
            throw new BusinessException(400, "用户名已存在");
        }
        Member member = new Member();
        member.setUsername(dto.username());
        member.setPassword(BCrypt.hashpw(dto.password()));
        // 昵称缺省用用户名，避免界面上出现「无昵称」用户
        member.setNickname(StrUtil.isBlank(dto.nickname()) ? dto.username() : dto.nickname());
        member.setStatus(STATUS_NORMAL);
        try {
            memberMapper.insert(member);
        } catch (DuplicateKeyException e) {
            // 查重与插入之间的并发窗口：靠 DB 唯一键兜底，统一转成业务提示
            throw new BusinessException(400, "用户名已存在");
        }
        return issue(member);
    }

    @Override
    public MemberInfoVO currentMember() {
        Member member = memberMapper.selectById(UserContext.getId());
        if (member == null) {
            throw new BusinessException(401, "登录状态已失效，请重新登录");
        }
        requireNormal(member);
        return toInfo(member);
    }

    private Member findByUsername(String username) {
        return memberMapper.selectOne(
                Wrappers.<Member>lambdaQuery().eq(Member::getUsername, username));
    }

    /** 封禁账号：已签发令牌在过期前也一律拦下 */
    private void requireNormal(Member member) {
        if (!Integer.valueOf(STATUS_NORMAL).equals(member.getStatus())) {
            throw new BusinessException(403, "该账号已被封禁");
        }
    }

    private MemberLoginVO issue(Member member) {
        String token = jwtUtils.createToken(member.getId(), UserType.MEMBER);
        return new MemberLoginVO(token, toInfo(member));
    }

    private MemberInfoVO toInfo(Member member) {
        return new MemberInfoVO(
                member.getId(), member.getUsername(), member.getNickname(),
                member.getAvatar(), member.getIntro());
    }
}
