package com.lelcc.dto;

/**
 * 上传结果。库里落的是 key（objectKey）；url 是后端按当前 provider 现拼出来的可访问地址，
 * 前端提交时保存 key，显示时用 url（或再用公共基址把 key 拼一次）。
 */
public record UploadVO(String key, String url, String name, long size) {
}
