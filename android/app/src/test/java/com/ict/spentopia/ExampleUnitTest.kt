package com.ict.spentopia // 이 파일이 속한 패키지 위치를 적음

import org.junit.Test // Test 기능을 가져옴

import org.junit.Assert.* // * 기능을 가져옴

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest { // ExampleUnitTest 기능을 묶어둔 클래스 시작
    @Test // 이 코드에 특별한 역할을 붙이는 표시
    fun addition_isCorrect() { // addition_isCorrect 함수를 선언함
        assertEquals(4, 2 + 2) // assert Equals 함수를 실행함
    }
}