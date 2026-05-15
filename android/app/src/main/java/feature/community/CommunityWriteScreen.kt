package com.ict.spentopia.feature.community // 이 파일이 속한 패키지 위치를 적음

// ------------------------------------------------------------
// CommunityWriteScreen.kt
// ------------------------------------------------------------
// 이 파일은 커뮤니티 글쓰기 화면을 담당합니다.
//
// 현재 단계 목표:
// 1. 제목 / 내용 / 카테고리를 입력합니다.
// 2. 등록 버튼 클릭 시 바깥으로 데이터를 전달합니다.
// 3. 바깥(AppNavGraph)에서 실제 리스트에 추가하도록 합니다.
// ------------------------------------------------------------

import android.content.Context // 현재 화면 정보 타입을 가져옴
import android.net.Uri // 이미지 주소 타입을 가져옴
import android.graphics.Bitmap // 이미지 데이터 타입을 가져옴
import androidx.activity.compose.rememberLauncherForActivityResult // rememberLauncherForActivityResult 기능을 가져옴
import androidx.activity.result.contract.ActivityResultContracts // ActivityResultContracts 기능을 가져옴
import androidx.compose.foundation.background // background 기능을 가져옴
import androidx.compose.foundation.clickable // clickable 기능을 가져옴
import androidx.compose.foundation.layout.Arrangement // Arrangement 기능을 가져옴
import androidx.compose.foundation.layout.Box // 겹쳐서 배치하는 레이아웃을 가져옴
import androidx.compose.foundation.layout.Column // 세로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.ExperimentalLayoutApi // ExperimentalLayoutApi 기능을 가져옴
import androidx.compose.foundation.layout.FlowRow // FlowRow 기능을 가져옴
import androidx.compose.foundation.layout.PaddingValues // PaddingValues 기능을 가져옴
import androidx.compose.foundation.layout.Row // 가로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.Spacer // Spacer 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxSize // fillMaxSize 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxWidth // fillMaxWidth 기능을 가져옴
import androidx.compose.foundation.layout.height // height 기능을 가져옴
import androidx.compose.foundation.layout.padding // padding 기능을 가져옴
import androidx.compose.foundation.layout.size // size 기능을 가져옴
import androidx.compose.foundation.layout.width // width 기능을 가져옴
import androidx.compose.foundation.layout.widthIn // widthIn 기능을 가져옴
import androidx.compose.foundation.rememberScrollState // rememberScrollState 기능을 가져옴
import androidx.compose.foundation.shape.RoundedCornerShape // RoundedCornerShape 기능을 가져옴
import androidx.compose.foundation.verticalScroll // verticalScroll 기능을 가져옴
import androidx.compose.material.icons.Icons // Icons 기능을 가져옴
import androidx.compose.material.icons.filled.AttachFile // AttachFile 기능을 가져옴
import androidx.compose.material.icons.filled.DeleteOutline // DeleteOutline 기능을 가져옴
import androidx.compose.material.icons.filled.PhotoCamera // PhotoCamera 기능을 가져옴
import androidx.compose.material.icons.filled.Settings // Settings 기능을 가져옴
import androidx.compose.material3.Icon // 아이콘 표시 컴포넌트를 가져옴
import androidx.compose.material3.Button // 버튼 컴포넌트를 가져옴
import androidx.compose.material3.ButtonDefaults // ButtonDefaults 기능을 가져옴
import androidx.compose.material3.Card // Card 기능을 가져옴
import androidx.compose.material3.CardDefaults // CardDefaults 기능을 가져옴
import androidx.compose.material3.OutlinedTextField // OutlinedTextField 기능을 가져옴
import androidx.compose.material3.OutlinedTextFieldDefaults // OutlinedTextFieldDefaults 기능을 가져옴
import androidx.compose.material3.MaterialTheme // MaterialTheme 기능을 가져옴
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴
import androidx.compose.runtime.getValue // by로 상태를 읽게 해줌
import androidx.compose.runtime.mutableStateOf // 화면 상태를 만드는 도구를 가져옴
import androidx.compose.runtime.remember // 값을 기억하는 Compose 도구를 가져옴
import androidx.compose.runtime.setValue // by로 상태를 바꾸게 해줌
import androidx.compose.ui.Alignment // Alignment 기능을 가져옴
import androidx.compose.ui.Modifier // UI 크기랑 여백 설정 도구를 가져옴
import androidx.compose.ui.graphics.Color // 색상 타입을 가져옴
import androidx.compose.ui.layout.ContentScale // ContentScale 기능을 가져옴
import androidx.compose.ui.platform.LocalContext // LocalContext 기능을 가져옴
import androidx.compose.ui.text.font.FontWeight // FontWeight 기능을 가져옴
import androidx.compose.ui.tooling.preview.Preview // Preview 기능을 가져옴
import androidx.compose.ui.unit.dp // 화면 크기 단위를 가져옴
import androidx.compose.ui.unit.sp // 글자 크기 단위를 가져옴
import coil.compose.AsyncImage // AsyncImage 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaDarkBackground // 앱 다크모드 배경색을 가져옴
import com.ict.spentopia.ui.theme.spentopiaAppButtonColor
import com.ict.spentopia.ui.theme.spentopiaAppButtonContentColor
import java.io.File // File 기능을 가져옴
import java.io.FileOutputStream // FileOutputStream 기능을 가져옴

@Composable // 이 함수가 화면 UI를 그린다는 표시
fun CommunityWriteScreen( // CommunityWriteScreen 함수를 선언함
    initialCategory: CommunityCategory = CommunityCategory.FREE_BOARD, // initialCategory 값을 받음
    initialContestId: String? = null, // initialContestId 값을 받음
    onSubmitClick: (CommunityCategory, String, String, Uri?, String?) -> Unit = { _, _, _, _, _ -> } // Unit 값을 정해줌
) { // 이 블록 안의 내용이 시작됨
    val context = LocalContext.current // 현재 화면 정보를 저장함

    // 제목 입력 상태입니다.
    var title by remember { mutableStateOf("") } // 화면에서 바뀔 제목을 저장함

    // 내용 입력 상태입니다.
    var content by remember { mutableStateOf("") } // 화면에서 바뀔 내용을 저장함

    // 선택된 카테고리 상태입니다.
    var selectedCategory by remember(initialCategory) { // 화면이 다시 그려져도 selectedCategory 값을 기억함
        mutableStateOf( // 화면 상태값을 만듦
            // 게시판 선택은 요청/콘테스트/자유만 허용합니다.
            // 그 외 값이 들어오면 기본값인 자유 게시판으로 돌립니다.
            if (initialCategory == CommunityCategory.REQUEST || // 조건이 맞는지 확인함
                initialCategory == CommunityCategory.AVATAR_CONTEST || // initialCategory 값을 정해줌
                initialCategory == CommunityCategory.FREE_BOARD // initialCategory 값을 정해줌
            ) initialCategory else CommunityCategory.FREE_BOARD
        )
    }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) } // 화면에서 바뀔 selectedImageUri 값을 저장함

    val imagePickerLauncher = rememberLauncherForActivityResult( // 화면이 다시 그려져도 imagePickerLauncher 값을 기억함
        contract = ActivityResultContracts.GetContent() // contract 값을 정해줌
    ) { uri ->
        // 앨범에서 고른 이미지를 그대로 저장합니다.
        // Uri는 미리보기와 서버 업로드 전 단계에서 재사용됩니다.
        selectedImageUri = uri // 이미지 주소를 selectedImageUri 값에 넣음
    }

    val cameraLauncher = rememberLauncherForActivityResult( // 화면이 다시 그려져도 cameraLauncher 값을 기억함
        contract = ActivityResultContracts.TakePicturePreview() // contract 값을 정해줌
    ) { bitmap: Bitmap? ->
        bitmap?.let { // 이 블록 안의 내용이 시작됨
            // 카메라 미리보기 Bitmap을 임시 파일 Uri로 바꿉니다.
            // 갤러리 이미지와 같은 방식으로 다루기 위해서입니다.
            selectedImageUri = saveBitmapToCacheUri( // selectedImageUri 값을 정해줌
                context = context, // 현재 화면 정보를 현재 화면 정보에 넣음
                bitmap = it, // it 값을 이미지 데이터에 넣음
                filePrefix = "community_camera" // filePrefix 값을 정해줌
            )
        }
    }

    Column( // 안쪽 UI를 세로로 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp) // verticalArrangement 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        CommunityWriteTopSection() // Community Write Top Section 함수를 실행함

        CommunityWriteCategoryCard( // 내용을 카드 모양으로 묶어서 보여줌
            selectedCategory = selectedCategory, // selectedCategory 값을 selectedCategory 값에 넣음
            onCategorySelected = { clickedCategory -> // onCategorySelected 때 실행할 함수를 정해줌
                selectedCategory = clickedCategory // clickedCategory 값을 selectedCategory 값에 넣음
            }
        )

        CommunityWriteTitleCard( // 내용을 카드 모양으로 묶어서 보여줌
            title = title, // 제목을 제목에 넣음
            onTitleChange = { newTitle -> // onTitleChange 때 실행할 함수를 정해줌
                title = newTitle // newTitle 값을 제목에 넣음
            }
        )

        CommunityWriteContentCard( // 내용을 카드 모양으로 묶어서 보여줌
            content = content, // 내용을 내용에 넣음
            onContentChange = { newContent -> // onContentChange 때 실행할 함수를 정해줌
                content = newContent // newContent 값을 내용에 넣음
            }
        )

        CommunityWriteAttachmentCard( // 내용을 카드 모양으로 묶어서 보여줌
            selectedImageUri = selectedImageUri, // selectedImageUri 값을 selectedImageUri 값에 넣음
            onPickImageClick = { // onPickImageClick 때 실행할 함수를 정해줌
                imagePickerLauncher.launch("image/*")
            },
            onTakePhotoClick = { // onTakePhotoClick 때 실행할 함수를 정해줌
                cameraLauncher.launch(null)
            },
            onRemoveImageClick = { // onRemoveImageClick 때 실행할 함수를 정해줌
                selectedImageUri = null // null 값을 selectedImageUri 값에 넣음
            }
        )

        CommunityWriteSubmitSection( // Community Write Submit Section 함수를 실행함
            // 빈 제목/내용이면 등록되지 않도록 아주 기본 검사를 넣습니다.
            isEnabled = title.isNotBlank() && content.isNotBlank(), // isEnabled인지 여부를 정해줌
            onSubmitClick = { // onSubmitClick 때 실행할 함수를 정해줌
                // 초보자용 설명:
                // trim()은 앞뒤 공백을 정리해주는 함수입니다.
                val trimmedTitle = title.trim() // trimmedTitle 값을 저장함
                val trimmedContent = content.trim() // trimmedContent 값을 저장함

                if (trimmedTitle.isNotEmpty() && trimmedContent.isNotEmpty()) { // 조건이 맞는지 확인함
                    onSubmitClick( // on Submit Click 함수를 실행함
                        selectedCategory,
                        trimmedTitle,
                        trimmedContent,
                        selectedImageUri,
                        if (selectedCategory == CommunityCategory.AVATAR_CONTEST) initialContestId else null // 조건이 맞는지 확인함
                    )
                }
            }
        )
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunityWriteTopSection() { // CommunityWriteTopSection 함수를 선언함
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = MaterialTheme.colorScheme.surface // containerColor 값을 정해줌
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(18.dp) // UI 크기나 여백 같은 모양을 정함
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "커뮤니티 글쓰기", // text 값을 정해줌
                fontSize = 20.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(6.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = "다른 사용자들과 공유하고 싶은 이야기를 자유롭게 작성해보세요.", // text 값을 정해줌
                fontSize = 13.sp, // fontSize 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant, // color 값을 정해줌
                lineHeight = 19.sp // lineHeight 값을 정해줌
            )
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
@OptIn(ExperimentalLayoutApi::class) // 이 코드에 특별한 역할을 붙이는 표시
private fun CommunityWriteCategoryCard( // CommunityWriteCategoryCard 함수를 선언함
    selectedCategory: CommunityCategory, // selectedCategory 값을 받음
    onCategorySelected: (CommunityCategory) -> Unit // onCategorySelected 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = MaterialTheme.colorScheme.surface // containerColor 값을 정해줌
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(18.dp) // UI 크기나 여백 같은 모양을 정함
        ) { // 이 블록 안의 내용이 시작됨
            Row( // 안쪽 UI를 가로로 배치함
                verticalAlignment = Alignment.CenterVertically, // verticalAlignment 값을 정해줌
                horizontalArrangement = Arrangement.spacedBy(8.dp) // horizontalArrangement 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Icon( // 화면에 아이콘을 보여줌
                    imageVector = Icons.Filled.Settings, // imageVector 값을 정해줌
                    contentDescription = null, // null 값을 contentDescription 값에 넣음
                    modifier = Modifier.size(18.dp), // UI 크기나 여백 같은 모양을 정함
                    tint = MaterialTheme.colorScheme.onSurfaceVariant // tint 값을 정해줌
                )
                Text( // 화면에 글자를 보여줌
                    text = "게시판", // text 값을 정해줌
                    fontSize = 15.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                )
            }

            Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함

            FlowRow( // 안쪽 UI를 가로로 배치함
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                horizontalArrangement = Arrangement.spacedBy(8.dp), // horizontalArrangement 값을 정해줌
                verticalArrangement = Arrangement.spacedBy(8.dp) // verticalArrangement 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                // 각 칩은 게시판 선택 버튼입니다.
                // 현재 선택된 칩만 primary 색으로 바뀝니다.
                listOf( // list Of 함수를 실행함
                    CommunityCategory.REQUEST,
                    CommunityCategory.AVATAR_CONTEST,
                    CommunityCategory.FREE_BOARD
                ).forEach { category ->
                    val isSelected = category == selectedCategory // 선택된 항목인지 저장함

                    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                            .widthIn(min = 88.dp) // .widthIn(min 값을 정해줌
                            .background(
                                color = if (isSelected) { // color 값을 정해줌
                                    MaterialTheme.colorScheme.primary
                                } else { // 이 블록 안의 내용이 시작됨
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(999.dp) // shape 값을 정해줌
                            )
                            .clickable { // 이 블록 안의 내용이 시작됨
                                onCategorySelected(category) // on Category Selected 함수를 실행함
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp) // .padding(horizontal 값을 정해줌
                    ) { // 이 블록 안의 내용이 시작됨
                        Text( // 화면에 글자를 보여줌
                            text = category.label, // text 값을 정해줌
                            fontSize = 12.sp, // fontSize 값을 정해줌
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, // fontWeight 값을 정해줌
                            color = if (isSelected) { // color 값을 정해줌
                                MaterialTheme.colorScheme.onPrimary
                            } else { // 이 블록 안의 내용이 시작됨
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunityWriteTitleCard( // CommunityWriteTitleCard 함수를 선언함
    title: String, // 제목을 받음
    onTitleChange: (String) -> Unit // onTitleChange 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = MaterialTheme.colorScheme.surface // containerColor 값을 정해줌
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(18.dp) // UI 크기나 여백 같은 모양을 정함
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "제목", // text 값을 정해줌
                fontSize = 15.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함

            OutlinedTextField( // 사용자가 입력할 칸을 만듦
                value = title, // 제목을 입력값에 넣음
                onValueChange = onTitleChange, // onTitleChange 때 실행할 함수를 onValueChange 때 실행할 함수에 넣음
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                placeholder = { // placeholder 값을 정해줌
                    Text( // 화면에 글자를 보여줌
                        text = "제목을 입력하세요", // text 값을 정해줌
                        color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                    )
                },
                shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
                singleLine = true, // true 값을 singleLine 값에 넣음
                colors = OutlinedTextFieldDefaults.colors( // 사용자가 입력할 칸을 만듦
                    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant, // focusedBorderColor 값을 정해줌
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, // unfocusedBorderColor 값을 정해줌
                    focusedTextColor = MaterialTheme.colorScheme.onSurface, // focusedTextColor 값을 정해줌
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface, // unfocusedTextColor 값을 정해줌
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant, // focusedPlaceholderColor 값을 정해줌
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant, // unfocusedPlaceholderColor 값을 정해줌
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, // focusedContainerColor 값을 정해줌
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant // unfocusedContainerColor 값을 정해줌
                )
            )
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunityWriteContentCard( // CommunityWriteContentCard 함수를 선언함
    content: String, // 내용을 받음
    onContentChange: (String) -> Unit // onContentChange 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = MaterialTheme.colorScheme.surface // containerColor 값을 정해줌
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(18.dp) // UI 크기나 여백 같은 모양을 정함
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "내용", // text 값을 정해줌
                fontSize = 15.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함

            OutlinedTextField( // 사용자가 입력할 칸을 만듦
                value = content, // 내용을 입력값에 넣음
                onValueChange = onContentChange, // onContentChange 때 실행할 함수를 onValueChange 때 실행할 함수에 넣음
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .fillMaxWidth()
                    .height(220.dp),
                placeholder = { // placeholder 값을 정해줌
                    Text( // 화면에 글자를 보여줌
                        text = "내용을 입력하세요", // text 값을 정해줌
                        color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                    )
                },
                shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
                minLines = 8, // minLines 값을 정해줌
                colors = OutlinedTextFieldDefaults.colors( // 사용자가 입력할 칸을 만듦
                    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant, // focusedBorderColor 값을 정해줌
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, // unfocusedBorderColor 값을 정해줌
                    focusedTextColor = MaterialTheme.colorScheme.onSurface, // focusedTextColor 값을 정해줌
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface, // unfocusedTextColor 값을 정해줌
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant, // focusedPlaceholderColor 값을 정해줌
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant, // unfocusedPlaceholderColor 값을 정해줌
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, // focusedContainerColor 값을 정해줌
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant // unfocusedContainerColor 값을 정해줌
                )
            )
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunityWriteAttachmentCard( // CommunityWriteAttachmentCard 함수를 선언함
    selectedImageUri: Uri?, // selectedImageUri 값을 받음
    onPickImageClick: () -> Unit, // onPickImageClick 때 실행할 함수를 받음
    onTakePhotoClick: () -> Unit, // onTakePhotoClick 때 실행할 함수를 받음
    onRemoveImageClick: () -> Unit // onRemoveImageClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = MaterialTheme.colorScheme.surface // containerColor 값을 정해줌
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(18.dp) // UI 크기나 여백 같은 모양을 정함
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "첨부파일", // text 값을 정해줌
                fontSize = 15.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함

            Row( // 안쪽 UI를 가로로 배치함
                horizontalArrangement = Arrangement.spacedBy(8.dp), // horizontalArrangement 값을 정해줌
                modifier = Modifier.fillMaxWidth() // UI 크기나 여백 같은 모양을 정함
            ) { // 이 블록 안의 내용이 시작됨
                // 이미지 앨범 업로드 버튼입니다.
                // image/*만 허용해서 사진만 고를 수 있습니다.
                Button( // 누를 수 있는 버튼을 만듦
                    onClick = onPickImageClick, // onPickImageClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                    modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
                    shape = RoundedCornerShape(12.dp), // shape 값을 정해줌
                    colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                        containerColor = MaterialTheme.colorScheme.surfaceVariant, // containerColor 값을 정해줌
                        contentColor = MaterialTheme.colorScheme.onSurface // contentColor 값을 정해줌
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp) // contentPadding 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Icon( // 화면에 아이콘을 보여줌
                        imageVector = Icons.Filled.AttachFile, // imageVector 값을 정해줌
                        contentDescription = null, // null 값을 contentDescription 값에 넣음
                        modifier = Modifier.size(18.dp) // UI 크기나 여백 같은 모양을 정함
                    )
                    Spacer(modifier = Modifier.width(6.dp)) // UI 크기나 여백 같은 모양을 정함
                    Text( // 화면에 글자를 보여줌
                        text = "업로드", // text 값을 정해줌
                        fontSize = 13.sp, // fontSize 값을 정해줌
                        fontWeight = FontWeight.SemiBold // fontWeight 값을 정해줌
                    )
                }

                // 카메라 촬영 버튼입니다.
                // 촬영한 이미지는 임시 Uri로 바꿔서 같은 흐름으로 처리합니다.
                Button( // 누를 수 있는 버튼을 만듦
                    onClick = onTakePhotoClick, // onTakePhotoClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                    modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
                    shape = RoundedCornerShape(12.dp), // shape 값을 정해줌
                    colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                        containerColor = MaterialTheme.colorScheme.primaryContainer, // containerColor 값을 정해줌
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer // contentColor 값을 정해줌
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp) // contentPadding 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Icon( // 화면에 아이콘을 보여줌
                        imageVector = Icons.Filled.PhotoCamera, // imageVector 값을 정해줌
                        contentDescription = null, // null 값을 contentDescription 값에 넣음
                        modifier = Modifier.size(18.dp) // UI 크기나 여백 같은 모양을 정함
                    )
                    Spacer(modifier = Modifier.width(6.dp)) // UI 크기나 여백 같은 모양을 정함
                    Text( // 화면에 글자를 보여줌
                        text = "카메라", // text 값을 정해줌
                        fontSize = 13.sp, // fontSize 값을 정해줌
                        fontWeight = FontWeight.SemiBold // fontWeight 값을 정해줌
                    )
                }
            }

            if (selectedImageUri != null) { // 조건이 맞는지 확인함
                Spacer(modifier = Modifier.height(14.dp)) // UI 크기나 여백 같은 모양을 정함

                Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                    modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant, // color 값을 정해줌
                            shape = RoundedCornerShape(14.dp) // shape 값을 정해줌
                        )
                ) { // 이 블록 안의 내용이 시작됨
                    AsyncImage( // 화면에 이미지를 보여줌
                        model = selectedImageUri, // selectedImageUri 값을 model 값에 넣음
                        contentDescription = "첨부 이미지 미리보기", // contentDescription 값을 정해줌
                        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                            .fillMaxSize()
                            .padding(1.dp),
                        contentScale = ContentScale.Crop // contentScale 값을 정해줌
                    )
                }

                Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함

                Button( // 누를 수 있는 버튼을 만듦
                    onClick = onRemoveImageClick, // onRemoveImageClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                    shape = RoundedCornerShape(10.dp), // shape 값을 정해줌
                    colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                        containerColor = MaterialTheme.colorScheme.errorContainer, // containerColor 값을 정해줌
                        contentColor = MaterialTheme.colorScheme.onErrorContainer // contentColor 값을 정해줌
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp) // contentPadding 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Icon( // 화면에 아이콘을 보여줌
                        imageVector = Icons.Filled.DeleteOutline, // imageVector 값을 정해줌
                        contentDescription = null, // null 값을 contentDescription 값에 넣음
                        modifier = Modifier.size(17.dp) // UI 크기나 여백 같은 모양을 정함
                    )
                    Spacer(modifier = Modifier.width(6.dp)) // UI 크기나 여백 같은 모양을 정함
                    Text( // 화면에 글자를 보여줌
                        text = "첨부 이미지 삭제", // text 값을 정해줌
                        fontSize = 12.sp, // fontSize 값을 정해줌
                        fontWeight = FontWeight.Bold // fontWeight 값을 정해줌
                    )
                }
            }
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunityWriteSubmitSection( // CommunityWriteSubmitSection 함수를 선언함
    isEnabled: Boolean, // isEnabled인지 여부를 받음
    onSubmitClick: () -> Unit // onSubmitClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val colorScheme = MaterialTheme.colorScheme // colorScheme 값을 저장함
    val isDark = isCommunityWriteDarkTheme() // 앱 설정 기준으로 다크모드인지 저장함
    val buttonColor = spentopiaAppButtonColor(isDark) // 등록 버튼색을 모드별로 분리함
    val buttonContentColor = spentopiaAppButtonContentColor(isDark)

    Button( // 누를 수 있는 버튼을 만듦
        onClick = onSubmitClick, // onSubmitClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
        enabled = isEnabled, // isEnabled인지 여부를 enabled 값에 넣음
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
        colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
            containerColor = Color.Transparent, // containerColor 값을 정해줌
            contentColor = buttonContentColor, // contentColor 값을 정해줌
            disabledContainerColor = MaterialTheme.colorScheme.outlineVariant, // disabledContainerColor 값을 정해줌
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant // disabledContentColor 값을 정해줌
        ),
        contentPadding = PaddingValues(0.dp) // contentPadding 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .background(
                    color = if (isEnabled) buttonColor else MaterialTheme.colorScheme.outlineVariant, // 버튼 내부 색을 단색으로 정함
                    shape = RoundedCornerShape(14.dp) // shape 값을 정해줌
                )
                .padding(vertical = 14.dp), // .padding(vertical 값을 정해줌
            contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "등록하기", // text 값을 정해줌
                fontSize = 15.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = if (isEnabled) buttonContentColor else MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
            )
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun isCommunityWriteDarkTheme(): Boolean { // 앱 테마 기준으로 글쓰기 다크모드 여부를 확인함
    return MaterialTheme.colorScheme.background == SpentopiaDarkBackground // 시스템 설정이 아니라 앱 설정 기준으로 판단함
}

private fun saveBitmapToCacheUri(context: Context, bitmap: Bitmap, filePrefix: String): Uri? { // 데이터를 저장하는 함수 시작
    return try { // 이 값을 함수 결과로 돌려줌
        // 카메라로 찍은 이미지를 앱 캐시 폴더에 저장합니다.
        // Uri가 있어야 화면 미리보기와 업로드가 가능해집니다.
        val cacheDir = File(context.cacheDir, "community_media").apply { // cacheDir 값을 저장함
            if (!exists()) mkdirs() // 조건이 맞는지 확인함
        }
        val file = File(cacheDir, "${filePrefix}_${System.currentTimeMillis()}.jpg") // 파일을 저장함
        FileOutputStream(file).use { out -> // File Output Stream 함수를 실행함
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        Uri.fromFile(file)
    } catch (_: Exception) { // 이 블록 안의 내용이 시작됨
        null
    }
}

@Preview(showBackground = true) // 미리보기에서 화면을 볼 수 있게 표시함
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunityWriteScreenPreview() { // CommunityWriteScreenPreview 함수를 선언함
    CommunityWriteScreen() // 커뮤니티 글쓰기 화면을 보여줌
}
