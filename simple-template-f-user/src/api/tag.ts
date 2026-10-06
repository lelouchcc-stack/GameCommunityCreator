import http from "../utils/request";

/** 与后端 TagVO 对应（用户端只关心 id 与标签名，类型分内容分类/主题） */
export interface TagItem {
    id: number
    boardId: number | null
    tagName: string
    /** 1内容类型(攻略/讨论/同人) 2主题 */
    tagType: number
    sort: number
}

/** 启用的内容分类：全局标签 + 指定板块的标签，匿名可读 */
export function listTags(boardId?: number) {
    return http.get<TagItem[], TagItem[]>("/app/tag/list", { params: { boardId } });
}
