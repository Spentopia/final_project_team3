package com.ict.spentopia.feature.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ict.spentopia.data.repository.WalletRepository
import com.ict.spentopia.data.repository.WalletRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    application: Application
) : AndroidViewModel(application) {

    // 지갑 저장소 객체입니다.
    private val walletRepository: WalletRepository = WalletRepositoryImpl(application)

    // 저장 진행 상태입니다.
    private val _isSavingWallet = MutableStateFlow(false)

    // 저장 진행 상태 공개용입니다.
    val isSavingWallet: StateFlow<Boolean> = _isSavingWallet

    // 저장 에러 상태입니다.
    private val _walletSaveError = MutableStateFlow<String?>(null)

    // 저장 에러 상태 공개용입니다.
    val walletSaveError: StateFlow<String?> = _walletSaveError

    fun saveWalletSession(
        walletAddress: String,
        walletProvider: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                _isSavingWallet.value = true
                _walletSaveError.value = null

                walletRepository.saveWallet(
                    address = walletAddress,
                    provider = walletProvider
                )

                onSuccess()
            } catch (e: Exception) {
                _walletSaveError.value = e.message ?: "지갑 정보 저장 실패"
            } finally {
                _isSavingWallet.value = false
            }
        }
    }
}