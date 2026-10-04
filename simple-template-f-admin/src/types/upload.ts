/** 与后端 UploadVO 对应：key 落库，url 是后端按当前存储 provider 现拼出的可访问地址 */
export interface UploadResult {
    key: string
    url: string
    name: string | null
    size: number
}

/** 与后端 StorageConfigVO 对应 */
export interface StorageConfig {
    provider: string
    publicBaseUrl: string
}
