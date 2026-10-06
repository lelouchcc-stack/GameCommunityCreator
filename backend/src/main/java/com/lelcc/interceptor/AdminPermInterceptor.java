package com.lelcc.interceptor;

import com.lelcc.common.BusinessException;
import com.lelcc.common.UserContext;
import com.lelcc.mapper.SysMenuMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 后台写操作的权限标识校验（RBAC 的"后端那一半"）。
 *
 * <h3>为什么要有它</h3>
 * <p>原先后台只在 {@link AuthInterceptor} 里校验"是不是一个有效的 ADMIN 令牌"，
 * 而 sys_menu.perms（system:user:list、board:list、post:audit…）只被前端用来显隐菜单与按钮。
 * 结果是：一个只该审核帖子的"运营"账号，拿着合法令牌就能调 /admin/user、/admin/role、
 * /admin/site/config 等任何后台接口——权限控制停留在界面上，属于越权写漏洞。</p>
 *
 * <h3>这次只做什么</h3>
 * <p><b>只拦"会改数据"的请求</b>（非 GET/HEAD/OPTIONS）。读接口仍然对所有已登录管理员开放：
 * 后台外壳、菜单树、站点外观都要读配置，按读权限拦会让只有部分角色的账号一进后台就报错——
 * 那是把"越权写"的问题换成了"后台不可用"的问题。写操作才是不可回滚的那一类。</p>
 *
 * <p>映射集中在本类的 {@link #RULES} 里，不在每个 Controller 上散着写注解：后台接口按模块成片命名，
 * 一份表更容易一眼看全，也更容易核对有没有漏。规则顺序有意义，**具体路径要排在宽路径之前**。</p>
 *
 * <h3>没配权限标识的写接口</h3>
 * <p>一律拒绝（fail-closed），提示里带上方法 + 路径，方便直接对照这张表补齐。
 * 例外是少数"跨模块公共写接口"（如图片上传），它们不属于某一个菜单，放行见 {@link #EXEMPT_PREFIXES}。</p>
 */
public class AdminPermInterceptor implements HandlerInterceptor {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    /** 跨模块公共写接口：图片上传被站点信息/轮播图/板块封面共用，没有归属某一个权限 */
    private static final List<String> EXEMPT_PREFIXES = List.of("/admin/upload");

    private record Rule(Pattern pattern, String perm) {
    }

    private static Rule rule(String regex, String perm) {
        return new Rule(Pattern.compile(regex), perm);
    }

    /** URL（不含 context path）→ 需要的权限标识。顺序即优先级，先匹配到先用 */
    private static final List<Rule> RULES = List.of(
            // 站点配置域
            rule("^/admin/site/(config|home-modules)$", "site:config:list"),
            rule("^/admin/site/component-types(/.*)?$", "site:component:list"),
            rule("^/admin/site/presets(/.*)?$", "site:preset:list"),
            rule("^/admin/site/template(/.*)?$", "site:template:list"),
            // 页面与其逻辑图是同一件事，共用页面权限
            rule("^/admin/site/pages(/.*)?$", "site:page:list"),
            rule("^/admin/banner(/.*)?$", "site:banner:list"),
            // 内容管理域：版本与进度阶段属于「内容维度」，必须排在 /admin/board 之前
            rule("^/admin/board/\\d+/(versions|stages)(/.*)?$", "dimension:list"),
            rule("^/admin/board(/.*)?$", "board:list"),
            rule("^/admin/tag(/.*)?$", "tag:list"),
            rule("^/admin/notice(/.*)?$", "notice:list"),
            rule("^/admin/post(/.*)?$", "post:audit"),
            rule("^/admin/report(/.*)?$", "report:list"),
            // 数据统计只有读接口，但规则表保持完整：将来加"导出"之类也自动受控
            rule("^/admin/stat(/.*)?$", "stat:view"),
            // 系统权限域
            rule("^/admin/user(/.*)?$", "system:user:list"),
            rule("^/admin/role(/.*)?$", "system:role:list"),
            rule("^/admin/menu(/.*)?$", "system:menu:list"),
            rule("^/admin/log(/.*)?$", "system:log:list"));

    private final SysMenuMapper sysMenuMapper;

    public AdminPermInterceptor(SysMenuMapper sysMenuMapper) {
        this.sysMenuMapper = sysMenuMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (SAFE_METHODS.contains(request.getMethod())) {
            return true;
        }
        String path = pathOf(request);
        for (String exempt : EXEMPT_PREFIXES) {
            if (path.startsWith(exempt)) {
                return true;
            }
        }
        String required = requiredPerm(path);
        if (required == null) {
            throw new BusinessException(403,
                    "该操作尚未配置权限标识，已拒绝：" + request.getMethod() + " " + path);
        }
        Long userId = UserContext.getId();
        if (userId != null && sysMenuMapper.selectPermsByUserId(userId).contains(required)) {
            return true;
        }
        throw new BusinessException(403, "当前账号没有该操作权限（需要 " + required + "）");
    }

    /** 去掉 context path 的请求路径，规则表按它匹配 */
    private String pathOf(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && uri.startsWith(context)) {
            return uri.substring(context.length());
        }
        return uri;
    }

    /** 取该路径需要的权限标识；没命中返回 null（由调用方 fail-closed） */
    private String requiredPerm(String path) {
        for (Rule rule : RULES) {
            if (rule.pattern().matcher(path).matches()) {
                return rule.perm();
            }
        }
        return null;
    }
}
