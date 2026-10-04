/** 与后端 BannerSlideVO 对应 */
export interface BannerSlide {
    id: number
    title: string | null
    subtitle: string | null
    image: string
    linkUrl: string | null
    sort: number
    status: number
    createTime: string
}

/** 新增 / 编辑轮播图表单：顺序不在这里改，走列表的「上移 / 下移」整组重排 */
export interface BannerSlideForm {
    title: string
    subtitle: string
    image: string
    linkUrl: string
}
