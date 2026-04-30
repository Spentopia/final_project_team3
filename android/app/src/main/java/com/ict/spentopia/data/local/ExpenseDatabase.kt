package com.ict.spentopia.data.local

// 앱 Room DB 정의임
// 소비 기록 테이블 저장/조회 담당
import android.content.Context

// Room 관련 import입니다.
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Room 데이터베이스 클래스임
@Database(
    entities = [ExpenseEntity::class], // 이 DB에서 사용할 테이블(Entity) 목록입니다.
    version = 3, // 현재 DB 버전입니다. Entity 컬럼이 바뀌면 버전도 올려야 Room 오류가 나지 않습니다.
    exportSchema = false // 스키마 내보내기를 사용하지 않겠다는 뜻입니다.
)
abstract class ExpenseDatabase : RoomDatabase() {

    // ExpenseDao 제공 함수
    abstract fun expenseDao(): ExpenseDao

    companion object {
        // DB 인스턴스 싱글톤용
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        // DB 인스턴스 반환함
        fun getDatabase(context: Context): ExpenseDatabase {
            // 이미 있으면 바로 반환
            return INSTANCE ?: synchronized(this) {
                // synchronized로 중복 생성 방지
                val instance = INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, // 메모리 누수 방지용
                    ExpenseDatabase::class.java, // Room DB 클래스
                    "expense_database" // DB 파일 이름
                ).fallbackToDestructiveMigration()
                    .build()

                // 새 인스턴스 저장
                INSTANCE = instance

                // 생성 인스턴스 반환
                instance
            }
        }
    }
}
