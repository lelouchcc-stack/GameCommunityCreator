package com.lelcc.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lelcc.annotation.OperLog;
import com.lelcc.common.LoginUser;
import com.lelcc.common.UserContext;
import com.lelcc.entity.SysOperLog;
import com.lelcc.entity.SysUser;
import com.lelcc.mapper.SysOperLogMapper;
import com.lelcc.mapper.SysUserMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 自动记录被 {@link OperLog} 标注的写操作。
 * 用 @Around 而不是 @AfterReturning，是因为失败的操作也要留痕（把异常信息记进 error_msg）。
 */
@Aspect
@Component
public class OperLogAspect {

    private static final int STATUS_SUCCESS = 1;
    private static final int STATUS_FAIL = 0;
    private static final int MAX_TEXT_LENGTH = 2000;

    /** 参数里出现这些字段名时，值一律替成 ***，否则登录、重置密码会把明文密码写进日志表 */
    private static final Pattern SENSITIVE_FIELD = Pattern.compile(
            "(\"(?:password|oldPassword|newPassword|confirmPassword)\"\\s*:\\s*)\"[^\"]*\"",
            Pattern.CASE_INSENSITIVE);

    @Resource
    private SysOperLogMapper sysOperLogMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private ObjectMapper objectMapper;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            save(joinPoint, operLog, start, STATUS_SUCCESS, null);
            return result;
        } catch (Throwable e) {
            save(joinPoint, operLog, start, STATUS_FAIL, e.getMessage());
            throw e;
        }
    }

    /** 记日志本身失败绝不能影响业务，所以整体兜一层异常 */
    private void save(ProceedingJoinPoint joinPoint, OperLog operLog, long start, int status, String errorMsg) {
        try {
            SysOperLog log = new SysOperLog();
            log.setModule(operLog.module());
            log.setOperation(operLog.operation());
            log.setMethod(joinPoint.getSignature().toShortString());
            log.setParams(buildParams(joinPoint.getArgs()));
            log.setIp(clientIp());
            log.setStatus(status);
            log.setErrorMsg(truncate(errorMsg));
            log.setCostTime(System.currentTimeMillis() - start);

            LoginUser loginUser = UserContext.get();
            if (loginUser != null) {
                log.setUserId(loginUser.id());
                SysUser user = sysUserMapper.selectById(loginUser.id());
                log.setUsername(user == null ? null : user.getUsername());
            }
            sysOperLogMapper.insert(log);
        } catch (Exception ignored) {
            // 日志写失败就放弃，业务结果照常返回
        }
    }

    private String buildParams(Object[] args) {
        List<Object> printable = Arrays.stream(args)
                .filter(arg -> !(arg instanceof HttpServletRequest))
                .toList();

        String json;
        try {
            // 用 Jackson 而不是 Hutool：record 类型的 DTO 只有 name() 这种访问器，Hutool 会序列化成空对象
            json = objectMapper.writeValueAsString(printable);
        } catch (Exception e) {
            json = String.valueOf(printable);
        }
        return truncate(SENSITIVE_FIELD.matcher(json).replaceAll("$1\"***\""));
    }

    private String clientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String text) {
        if (text == null || text.length() <= MAX_TEXT_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_TEXT_LENGTH);
    }
}
