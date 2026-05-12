package com.ict.spentopia.feature.auth.connector // 이 파일이 속한 패키지 위치를 적음

import android.net.Uri // 이미지 주소 타입을 가져옴
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender // ActivityResultSender 기능을 가져옴
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity // ConnectionIdentity 기능을 가져옴
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter // MobileWalletAdapter 기능을 가져옴
import com.solana.mobilewalletadapter.clientlib.TransactionResult // TransactionResult 기능을 가져옴
import com.solana.mobilewalletadapter.clientlib.successPayload // successPayload 기능을 가져옴
import org.bitcoinj.core.Base58 // Base58 기능을 가져옴

class MwaSolflareConnector : SolanaWalletConnector { // MwaSolflareConnector 기능을 묶어둔 클래스 시작

    private val walletAdapter = MobileWalletAdapter( // 지갑 관련 값을 저장함
        connectionIdentity = ConnectionIdentity( // connectionIdentity 값을 정해줌
            identityUri = Uri.parse("https://spentopia.com"), // identityUri 값을 정해줌
            iconUri = Uri.parse("icon.png"), // iconUri 값을 정해줌
            identityName = "Spentopia" // identityName 값을 정해줌
        )
    )

    override suspend fun connect( // connect 함수를 선언함
        walletActivityResultSender: ActivityResultSender // 지갑 관련 값을 받음
    ): WalletConnectionResult { // 이 블록 안의 내용이 시작됨
        return when (val result = walletAdapter.connect(walletActivityResultSender)) { // 이 값을 함수 결과로 돌려줌
            is TransactionResult.Success -> { // 이 블록 안의 내용이 시작됨
                val account = result.authResult.accounts.firstOrNull() // account 값을 저장함
                if (account == null) { // 조건이 맞는지 확인함
                    WalletConnectionResult.Failure("지갑 계정을 찾을 수 없습니다.")
                } else { // 이 블록 안의 내용이 시작됨
                    WalletConnectionResult.Success(
                        walletAddress = Base58.encode(account.publicKey) // 지갑 주소를 정해줌
                    )
                }
            }

            is TransactionResult.NoWalletFound -> { // 이 블록 안의 내용이 시작됨
                WalletConnectionResult.Failure("Solflare 지갑 앱을 찾을 수 없습니다.")
            }

            is TransactionResult.Failure -> { // 이 블록 안의 내용이 시작됨
                WalletConnectionResult.Failure(
                    result.e.message ?: "Solflare 지갑 연결 실패"
                )
            }
        }
    }

    override suspend fun signMessage( // signMessage 함수를 선언함
        walletActivityResultSender: ActivityResultSender, // 지갑 관련 값을 받음
        message: ByteArray // 메시지를 받음
    ): WalletSignResult { // 이 블록 안의 내용이 시작됨
        return try { // 이 값을 함수 결과로 돌려줌
            when ( // 값 종류에 따라 실행할 코드를 나눔
                val result = walletAdapter.transact(walletActivityResultSender) { authResult -> // result 값을 저장함
                    signMessagesDetached( // sign Messages Detached 함수를 실행함
                        arrayOf(message), // array Of 함수를 실행함
                        arrayOf(authResult.accounts.first().publicKey) // array Of 함수를 실행함
                    )
                }
            ) { // 이 블록 안의 내용이 시작됨
                is TransactionResult.Success -> { // 이 블록 안의 내용이 시작됨
                    val signatureBytes = result.successPayload // signatureBytes 값을 저장함
                        ?.messages
                        ?.firstOrNull()
                        ?.signatures
                        ?.firstOrNull()

                    if (signatureBytes == null) { // 조건이 맞는지 확인함
                        WalletSignResult.Failure("서명 결과가 비어 있습니다.")
                    } else { // 이 블록 안의 내용이 시작됨
                        WalletSignResult.Success(
                            signature = Base58.encode(signatureBytes) // 지갑 서명값을 정해줌
                        )
                    }
                }

                is TransactionResult.NoWalletFound -> { // 이 블록 안의 내용이 시작됨
                    WalletSignResult.Failure("Solflare 지갑 앱을 찾을 수 없습니다.")
                }

                is TransactionResult.Failure -> { // 이 블록 안의 내용이 시작됨
                    WalletSignResult.Failure(
                        result.e.message ?: "Solflare 지갑 서명 실패"
                    )
                }
            }
        } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
            WalletSignResult.Failure(e.message ?: "Solflare 지갑 서명 중 오류")
        }
    }
}
