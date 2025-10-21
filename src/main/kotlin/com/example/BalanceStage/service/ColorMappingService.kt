package com.example.BalanceStage.service

import javafx.scene.paint.Color

/**
 * 색상 매핑 서비스 인터페이스
 * SRP: 값을 색상으로 변환하는 책임만 담당
 */
interface ColorMappingService {
    /**
     * 편차 값을 무지개 그라데이션 색상으로 매핑
     * @param deviation 평면으로부터의 편차 (signed distance)
     * @param maxDeviation 최대 편차 절댓값
     * @return JavaFX Color
     */
    fun mapDeviationToColor(deviation: Double, maxDeviation: Double): Color
}
