package com.lelcc.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lelcc.common.BusinessException;
import com.lelcc.common.PageResult;
import com.lelcc.common.UserContext;
import com.lelcc.dto.AppPostDetailVO;
import com.lelcc.dto.AppPostItemVO;
import com.lelcc.dto.HotPostVO;
import com.lelcc.dto.PostCreateDTO;
import com.lelcc.dto.PostCreateVO;
import com.lelcc.entity.Board;
import com.lelcc.entity.BoardVersion;
import com.lelcc.entity.Member;
import com.lelcc.entity.Post;
import com.lelcc.entity.ProgressStage;
import com.lelcc.entity.Tag;
import com.lelcc.mapper.BoardMapper;
import com.lelcc.mapper.BoardVersionMapper;
import com.lelcc.mapper.MemberMapper;
import com.lelcc.mapper.PostMapper;
import com.lelcc.mapper.ProgressStageMapper;
import com.lelcc.mapper.TagMapper;
import com.lelcc.service.PostService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class PostServiceImpl implements PostService {

    private static final int MAX_LIMIT = 50;
    private static final int DEFAULT_LIMIT = 10;
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_PUBLISHED = 1;
    /** 板块/版本/阶段/标签的启用状态都是 1 */
    private static final int ENABLED = 1;
    /** 单帖最多挂几个标签，防止一次塞几十个 */
    private static final int MAX_TAGS = 5;
    private static final int MAX_TAG_IDS = 20;

    @Resource
    private PostMapper postMapper;

    @Resource
    private BoardMapper boardMapper;

    @Resource
    private BoardVersionMapper boardVersionMapper;

    @Resource
    private ProgressStageMapper progressStageMapper;

    @Resource
    private TagMapper tagMapper;

    @Resource
    private MemberMapper memberMapper;

    @Override
    public List<HotPostVO> listHot(int limit) {
        return postMapper.selectHotList(normalize(limit));
    }

    @Override
    public PageResult<AppPostItemVO> pagePublished(Long boardId, Long versionId, Long stageId, Long tagId,
                                                   String keyword, String sort, long pageNum, long pageSize) {
        Page<AppPostItemVO> page = new Page<>(pageNum, pageSize);
        PageResult<AppPostItemVO> result = PageResult.of(
                postMapper.selectAppPostPage(page, boardId, versionId, stageId, tagId, keyword, sort));
        result.list().forEach(this::markExpired);
        return result;
    }

    @Override
    public AppPostDetailVO detail(Long postId) {
        AppPostDetailVO detail = postMapper.selectAppDetail(postId);
        if (detail == null || !Integer.valueOf(STATUS_PUBLISHED).equals(detail.getStatus())) {
            throw new BusinessException(404, "内容不可见或已下架");
        }
        postMapper.incrementViewCount(postId);
        // 返回值与刚写入的浏览数保持一致，前端无需再拉一次
        detail.setViewCount((detail.getViewCount() == null ? 0 : detail.getViewCount()) + 1);
        detail.setTags(postMapper.selectTagNames(postId));
        detail.setImageUrls(parseImageUrls(detail.getImageUrlsJson()));
        detail.setImageUrlsJson(null);
        markExpired(detail);
        return detail;
    }

    @Override
    public PageResult<AppPostItemVO> pageMine(long pageNum, long pageSize) {
        Long memberId = UserContext.getId();
        if (memberId == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }
        Page<AppPostItemVO> page = new Page<>(pageNum < 1 ? 1 : pageNum,
                pageSize < 1 ? DEFAULT_LIMIT : Math.min(pageSize, MAX_LIMIT));
        PageResult<AppPostItemVO> result = PageResult.of(
                postMapper.selectMemberPostPage(page, memberId));
        result.list().forEach(this::markExpired);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostCreateVO create(PostCreateDTO dto) {
        Long memberId = UserContext.getId();
        if (memberId == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }
        // 封禁玩家不该能产内容。令牌本身不带账号状态，登录后状态才可能被改，
        // 所以写入前再查一次——只在登录时校验的话，封禁到令牌过期为止是形同虚设的
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException(401, "登录状态已失效，请重新登录");
        }
        if (!Integer.valueOf(ENABLED).equals(member.getStatus())) {
            throw new BusinessException(403, "该账号已被封禁，无法发帖");
        }

        Board board = boardMapper.selectById(dto.boardId());
        if (board == null || !Integer.valueOf(ENABLED).equals(board.getStatus())) {
            throw new BusinessException(400, "该板块暂未开放，无法发帖");
        }
        requireBoardVersion(dto.versionId(), board.getId());
        requireProgressStage(dto.stageId(), board.getId());
        List<Long> tagIds = usableTagIds(dto.tagIds(), board.getId());

        Post post = new Post();
        post.setBoardId(board.getId());
        post.setMemberId(memberId);
        post.setVersionId(dto.versionId());
        post.setStageId(dto.stageId());
        post.setTitle(StrUtil.trim(dto.title()));
        post.setContent(dto.content());
        post.setCover(StrUtil.trimToNull(dto.cover()));
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setFavoriteCount(0);
        post.setCommentCount(0);
        // 一律待审：用例 05 明确"以待审核状态保存，待管理员审核通过后对其他玩家可见"
        post.setStatus(STATUS_PENDING);
        post.setIsTop(0);
        postMapper.insert(post);

        if (!tagIds.isEmpty()) {
            postMapper.insertPostTags(post.getId(), tagIds);
        }
        return new PostCreateVO(post.getId(), post.getStatus());
    }

    /** 版本必须是该板块下启用中的版本——跨板块挂版本会让"版本过期"判定失真 */
    private void requireBoardVersion(Long versionId, Long boardId) {
        BoardVersion version = versionId == null ? null : boardVersionMapper.selectById(versionId);
        if (version == null || !boardId.equals(version.getBoardId())
                || !Integer.valueOf(ENABLED).equals(version.getStatus())) {
            throw new BusinessException(400, "关联的游戏版本不存在或已停用");
        }
    }

    /** 进度阶段可选；传了就必须是同一板块下启用中的阶段 */
    private void requireProgressStage(Long stageId, Long boardId) {
        if (stageId == null) {
            return;
        }
        ProgressStage stage = progressStageMapper.selectById(stageId);
        if (stage == null || !boardId.equals(stage.getBoardId())
                || !Integer.valueOf(ENABLED).equals(stage.getStatus())) {
            throw new BusinessException(400, "关联的进度阶段不存在或已停用");
        }
    }

    /**
     * 过滤出真正可用的标签：启用中的、且要么是全局标签、要么属于本板块。
     * 传了不存在的 id 直接报错而不是静默丢弃——静默丢弃会让作者以为标签挂上了。
     */
    private List<Long> usableTagIds(List<Long> tagIds, Long boardId) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        Set<Long> unique = new LinkedHashSet<>(tagIds);
        if (unique.size() > MAX_TAGS || tagIds.size() > MAX_TAG_IDS) {
            throw new BusinessException(400, "最多选择 " + MAX_TAGS + " 个标签");
        }
        List<Tag> found = tagMapper.selectList(Wrappers.<Tag>lambdaQuery()
                .in(Tag::getId, unique)
                .eq(Tag::getStatus, ENABLED)
                .and(w -> w.isNull(Tag::getBoardId).or().eq(Tag::getBoardId, boardId)));
        if (found.size() != unique.size()) {
            throw new BusinessException(400, "选择的标签里有不存在或不属于该板块的项");
        }
        return List.copyOf(unique);
    }

    /** 帖子版本非空且与板块当前版本不一致时算过期；通用帖（版本为空）不算 */
    private void markExpired(AppPostItemVO item) {
        Long current = item.getCurrentVersionId();
        Long version = item.getVersionId();
        item.setExpired(current != null && version != null && !version.equals(current));
    }

    private void markExpired(AppPostDetailVO detail) {
        Long current = detail.getCurrentVersionId();
        Long version = detail.getVersionId();
        detail.setExpired(current != null && version != null && !version.equals(current));
    }

    /** image_urls 存的是 JSON 数组字符串，解析失败按空处理，不影响详情展示 */
    private List<String> parseImageUrls(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return JSONUtil.toList(json, String.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    private int normalize(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
