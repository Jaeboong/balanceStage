package com.example.BalanceStage.service

import javafx.scene.Group
import javafx.scene.SubScene

/**
 * 3D 등고선 시각화 서비스 인터페이스
 * SRP: 3D 등고선 뷰 생성만 담당
 */
interface ContourVisualizationService {
    /**
     * 포인트 데이터로부터 3D 등고선 뷰를 생성
     * @param points 3D 포인트 리스트 [(x, y, z), ...]
     * @param width SubScene 너비
     * @param height SubScene 높이
     * @return 생성된 SubScene
     */
    fun createContourView(
        points: List<Triple<Double, Double, Double>>,
        width: Double,
        height: Double
    ): SubScene
}
