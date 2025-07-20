package com.example.BalanceStage.api

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/positions")
class PositionController {

    // 데이터 클래스: 휠 각도 정보
    data class WheelPosition(
        val firstWheel: Double,
        val secondWheel: Double
    )

    // 현재 휠 각도를 저장할 변수
    private var currentPosition: WheelPosition = WheelPosition(0.0, 0.0)

    // POST 요청으로 새로운 각도 저장
    @PostMapping
    fun updatePosition(@RequestBody newPosition: WheelPosition) {
        currentPosition = newPosition
        println("수신됨: 1st=${newPosition.firstWheel}°, 2nd=${newPosition.secondWheel}°")
        println("현재 currentPosition 인스턴스 해시: ${System.identityHashCode(this)}")

    }

    // GET 요청으로 현재 각도 반환
    @GetMapping
    fun getPosition(): WheelPosition {
        println("GET currentPosition 인스턴스 해시: ${System.identityHashCode(this)}")
        println("반환 중: $currentPosition")
        return currentPosition
    }

    init {
        println("PositionController 등록됨 (컨트롤러 정상 인식됨)")
    }
}
