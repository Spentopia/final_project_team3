package com.ict.spentopia // 이 파일이 속한 패키지 위치를 적음

import androidx.test.platform.app.InstrumentationRegistry // InstrumentationRegistry 기능을 가져옴
import androidx.test.ext.junit.runners.AndroidJUnit4 // AndroidJUnit4 기능을 가져옴

import org.junit.Test // Test 기능을 가져옴
import org.junit.runner.RunWith // RunWith 기능을 가져옴

import org.junit.Assert.* // * 기능을 가져옴

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class) // 이 코드에 특별한 역할을 붙이는 표시
class ExampleInstrumentedTest { // ExampleInstrumentedTest 기능을 묶어둔 클래스 시작
    @Test // 이 코드에 특별한 역할을 붙이는 표시
    fun useAppContext() { // useAppContext 함수를 선언함
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext // appContext 값을 저장함
        assertEquals("com.ict.spentopia", appContext.packageName) // assert Equals 함수를 실행함
    }
}