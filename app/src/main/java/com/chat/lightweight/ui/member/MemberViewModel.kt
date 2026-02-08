package com.chat.lightweight.ui.member

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chat.lightweight.data.model.MemberItem
import com.chat.lightweight.data.repository.MemberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 成员管理UI状态
 */
data class MemberManagementUiState(
    val isLoading: Boolean = false,
    val members: List<MemberItem> = emptyList(),
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val isAdmin: Boolean = false
)

/**
 * 成员管理ViewModel
 * 遵循单一职责原则，仅负责成员管理的业务逻辑
 */
class MemberViewModel(
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemberManagementUiState())
    val uiState: StateFlow<MemberManagementUiState> = _uiState.asStateFlow()

    /**
     * 初始化（检查管理员权限）
     */
    fun init(userId: String, isAdmin: Boolean) {
        _uiState.value = _uiState.value.copy(isAdmin = isAdmin)

        if (!isAdmin) {
            _uiState.value = _uiState.value.copy(
                error = "仅管理员可访问"
            )
            return
        }

        loadMembers()
    }

    /**
     * 加载成员列表
     */
    fun loadMembers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            memberRepository.getMembers().collect { result ->
                result.onSuccess { members ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        members = members,
                        error = null
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "加载失败"
                    )
                }
            }
        }
    }

    /**
     * 刷新成员列表
     */
    fun refreshMembers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)

            memberRepository.getMembers().collect { result ->
                result.onSuccess { members ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        members = members,
                        error = null
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = exception.message ?: "刷新失败"
                    )
                }
            }
        }
    }

    /**
     * 更新成员备注
     */
    fun updateMemberNote(memberId: String, note: String) {
        viewModelScope.launch {
            val result = memberRepository.updateMemberNote(memberId, note)

            result.onSuccess {
                // 更新本地状态
                val updatedMembers = _uiState.value.members.map { member ->
                    if (member.id == memberId) {
                        member.copy(note = note)
                    } else {
                        member
                    }
                }
                _uiState.value = _uiState.value.copy(members = updatedMembers)
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    error = exception.message ?: "更新失败"
                )
            }
        }
    }

    /**
     * 删除成员
     */
    fun deleteMember(member: MemberItem) {
        viewModelScope.launch {
            val result = memberRepository.deleteMember(member.id)

            result.onSuccess {
                // 从列表中移除
                val updatedMembers = _uiState.value.members.filter { it.id != member.id }
                _uiState.value = _uiState.value.copy(members = updatedMembers)
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    error = exception.message ?: "删除失败"
                )
            }
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
