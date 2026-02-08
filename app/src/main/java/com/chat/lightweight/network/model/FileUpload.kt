package com.chat.lightweight.network.model

import com.google.gson.annotations.SerializedName

/**
 * 文件上传响应
 */
data class FileUploadResponse(
    @SerializedName("fileUrl")
    val fileUrl: String
)
