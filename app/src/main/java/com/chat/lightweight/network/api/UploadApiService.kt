package com.chat.lightweight.network.api

import com.chat.lightweight.network.model.FileUploadResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * 文件上传API
 * 单一职责：处理文件上传接口
 */
interface UploadApiService {

    /**
     * 上传文件
     * @param file 要上传的文件
     * @param userType 用户类型 (admin/member)
     */
    @Multipart
    @POST("upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Query("userType") userType: String = "member"
    ): Response<FileUploadResponse>
}
