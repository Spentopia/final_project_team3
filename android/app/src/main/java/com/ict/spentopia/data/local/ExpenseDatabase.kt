package com.ict.spentopia.data.local // 이 파일이 속한 패키지 위치를 적음

// 앱 Room DB 정의임
// 소비 기록 테이블 저장/조회 담당
import android.content.Context // 현재 화면 정보 타입을 가져옴

// Room 관련 import입니다.
import androidx.room.Database // Database 기능을 가져옴
import androidx.room.Room // Room 기능을 가져옴
import androidx.room.RoomDatabase // RoomDatabase 기능을 가져옴

// Room 데이터베이스 클래스임
@Database( // Room 데이터베이스 설정이라는 표시
    entities = [ExpenseEntity::class], // entities 값을 정해줌
    version = 3, // version 값을 정해줌
    exportSchema = false // false 값을 exportSchema 값에 넣음
)
abstract class ExpenseDatabase : RoomDatabase() { // 이 블록 안의 내용이 시작됨

    // ExpenseDao 제공 함수
    abstract fun expenseDao(): ExpenseDao

    companion object { // 이 블록 안의 내용이 시작됨
        // DB 인스턴스 싱글톤용
        @Volatile // 이 코드에 특별한 역할을 붙이는 표시
        private var INSTANCE: ExpenseDatabase? = null // 나중에 바뀔 수 있는 INSTANCE 값을 저장함

        // DB 인스턴스 반환함
        fun getDatabase(context: Context): ExpenseDatabase { // 데이터를 불러오는 함수 시작
            // 이미 있으면 바로 반환
            return INSTANCE ?: synchronized(this) { // 이 값을 함수 결과로 돌려줌
                // synchronized로 중복 생성 방지
                val instance = INSTANCE ?: Room.databaseBuilder( // instance 값을 저장함
                    context.applicationContext,
                    ExpenseDatabase::class.java, // 소비 내역 값을 받음
                    "expense_database"
                ).fallbackToDestructiveMigration()
                    .build()

                // 새 인스턴스 저장
                INSTANCE = instance // instance 값을 INSTANCE 값에 넣음

                // 생성 인스턴스 반환
                instance
            }
        }
    }
}
