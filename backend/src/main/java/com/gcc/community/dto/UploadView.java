package com.gcc.community.dto;

/** 图片上传的返回结果。 */
public record UploadView(String url, String name, long size, String contentType) {
}
