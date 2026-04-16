package com.ict.spentopia.data.local

// Context import입니다.
// Room 데이터베이스를 생성할 때 필요합니다.
import android.content.Context

// Room 관련 import입니다.
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Room 데이터베이스 클래스입니다.
@Database(
    entities = [ExpenseEntity::class], // 이 DB에서 사용할 테이블(Entity) 목록입니다.
    version = 2, // 현재 DB 버전입니다.  / / 버전 바꿀시 전에 있던 데이터들  날라감  원할시 버전 바꿔도 됌
    exportSchema = false // 스키마 내보내기를 사용하지 않겠다는 뜻입니다.
)
abstract class ExpenseDatabase : RoomDatabase() {

    // ExpenseDao를 외부에서 사용할 수 있게 제공하는 추상 함수입니다.
    abstract fun expenseDao(): ExpenseDao

    companion object {
        // DB 인스턴스를 싱글톤으로 유지하기 위한 변수입니다.
        // 앱 전체에서 하나만 만들어서 재사용하게 됩니다.
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        // 데이터베이스 인스턴스를 반환하는 함수입니다.
        // 이미 만들어진 DB가 있으면 그걸 반환하고,
        // 없으면 새로 생성해서 반환합니다.
        fun getDatabase(context: Context): ExpenseDatabase {
            // 이미 만들어진 인스턴스가 있으면 바로 반환합니다.
            return INSTANCE ?: synchronized(this) {
                // synchronized 블록 안에서 다시 한 번 확인합니다.
                // 멀티스레드 환경에서 중복 생성 방지용입니다.
                val instance = INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, // applicationContext를 사용해서 메모리 누수를 방지합니다.
                    ExpenseDatabase::class.java, // 생성할 Room DB 클래스입니다.
                    "expense_database" // 실제 기기 내부에 저장될 DB 파일 이름입니다.
                ).fallbackToDestructiveMigration()
                    .build()

                // 새로 만든 인스턴스를 INSTANCE에 저장합니다.
                INSTANCE = instance

                // 생성된 인스턴스를 반환합니다.
                instance
            }
        }
    }
}