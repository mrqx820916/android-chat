package com.chat.lightweight.data.repository

import android.content.Context
import com.chat.lightweight.data.local.PreferencesManager
import com.chat.lightweight.data.model.MemberItem
import com.chat.lightweight.network.NetworkRepository
import com.chat.lightweight.network.model.Member
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 成员管理仓库
 * 负责成员数据的获取和操作
 * 遵循单一职责原则，仅负责数据层操作
 */
class MemberRepository(
    private val networkRepository: NetworkRepository,
    private val context: Context
) {

    companion object {
        @Volatile
        private var instance: MemberRepository? = null

        fun getInstance(context: Context): MemberRepository {
            return instance ?: synchronized(this) {
                instance ?: MemberRepository(
                    NetworkRepository.getInstance(),
                    context.applicationContext
                ).also { instance = it }
            }
        }

        fun resetInstance() {
            instance = null
        }
    }

    /**
     * 获取成员列表（仅管理员）
     */
    fun getMembers(): Flow<Result<List<MemberItem>>> = flow {
        try {
            // 需要当前用户ID（管理员）
            val currentUserId = getCurrentUserId() ?: throw Exception("用户未登录")

            when (val result = networkRepository.getMembers()) {
                is com.chat.lightweight.network.ApiResponse.Success -> {
                    val members = result.data.map { it.toMemberItem() }
                    emit(Result.success(members))
                }
                is com.chat.lightweight.network.ApiResponse.Error -> {
                    emit(Result.failure(Exception(result.message)))
                }
                else -> {}
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * 更新成员备注
     */
    suspend fun updateMemberNote(memberId: String, note: String): Result<Unit> {
        return try {
            val currentUserId = getCurrentUserId() ?: throw Exception("用户未登录")

            when (val result = networkRepository.updateMemberNote(memberId, note)) {
                is com.chat.lightweight.network.ApiResponse.Success -> {
                    Result.success(Unit)
                }
                is com.chat.lightweight.network.ApiResponse.Error -> {
                    Result.failure(Exception(result.message))
                }
                else -> Result.failure(Exception("未知错误"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除成员
     */
    suspend fun deleteMember(memberId: String): Result<Unit> {
        return try {
            val currentUserId = getCurrentUserId() ?: throw Exception("用户未登录")

            when (val result = networkRepository.deleteMember(memberId)) {
                is com.chat.lightweight.network.ApiResponse.Success -> {
                    Result.success(Unit)
                }
                is com.chat.lightweight.network.ApiResponse.Error -> {
                    Result.failure(Exception(result.message))
                }
                else -> Result.failure(Exception("未知错误"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取当前用户ID
     * 从PreferencesManager获取
     */
    private suspend fun getCurrentUserId(): String? {
        return PreferencesManager.getInstance(context).getUserId()
    }
}

/**
 * Member转MemberItem扩展函数
 */
private fun Member.toMemberItem(): MemberItem {
    return MemberItem(
        id = id,
        username = username,
        note = adminNote ?: "",
        isOnline = isOnline == 1,
        messageCount = messageCount ?: 0,
        lastMessage = lastMessage ?: "",
        lastActive = lastActive ?: ""
    )
}
