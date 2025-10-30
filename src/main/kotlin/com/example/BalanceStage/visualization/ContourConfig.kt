package com.example.BalanceStage.visualization

/**
 * 등고선 렌더링 설정
 *
 * @property gridSize 그리드 해상도 (권장 범위: 60~260)
 *                    - 60: 빠른 실시간 렌더링 (3,600 셀)
 *                    - 160: 중간 품질 (25,600 셀)
 *                    - 260: 고품질 출력 (67,600 셀)
 *                    높을수록 부드럽지만 렌더링 시간 증가
 *
 * @property numContours 등고선 개수 (권장 범위: 8~24)
 *                       - 8: 간단한 시각화
 *                       - 16: 일반적인 용도
 *                       - 24: 상세한 등고선
 *
 * @property sigma 가우시안 스무딩 강도 (범위: 0.0~0.5)
 *                 - 0.0: 스무딩 없음 (원본 데이터 유지)
 *                 - 0.3: 중간 스무딩 (일반적인 용도)
 *                 - 0.5: 강한 스무딩 (노이즈 제거)
 *
 * @property colorMap 사용할 컬러맵 (기본값: TURBO)
 *
 * @property neutralDeviationThreshold 중립 색상 압축 임계값 (단위: mm 등 데이터 단위)
 *                                     - 이 값보다 작은 편차는 중립 색상(중간 색상) 근처로 압축
 *                                     - 0.0: 압축 없음 (전체 범위 사용)
 *                                     - 0.01: 0.01mm 미만의 작은 편차 강조
 *                                     작은 변화를 시각적으로 강조하고 싶을 때 사용
 */
data class ContourConfig(
    val gridSize: Int = 260,
    val numContours: Int = 24,
    val sigma: Double = 0.50,
    val colorMap: ColorMap = ColorMap.TURBO,
    val neutralDeviationThreshold: Double = 0.01
) {
    init {
        require(gridSize in 10..1000) { "gridSize must be between 10 and 1000, got $gridSize" }
        require(numContours in 1..100) { "numContours must be between 1 and 100, got $numContours" }
        require(sigma in 0.0..1.0) { "sigma must be between 0.0 and 1.0, got $sigma" }
        require(neutralDeviationThreshold >= 0.0) {
            "neutralDeviationThreshold must be non-negative, got $neutralDeviationThreshold"
        }
    }

    companion object {
        /**
         * 실시간 시뮬레이션에 최적화된 설정 (빠른 렌더링)
         */
        val REALTIME = ContourConfig(
            gridSize = 60,
            numContours = 12,
            sigma = 0.3,
            colorMap = ColorMap.TURBO
        )

        /**
         * 중간 품질 설정 (균형잡힌 속도와 품질)
         */
        val BALANCED = ContourConfig(
            gridSize = 160,
            numContours = 16,
            sigma = 0.4,
            colorMap = ColorMap.TURBO
        )

        /**
         * 고품질 출력용 설정 (최고 품질)
         */
        val HIGH_QUALITY = ContourConfig(
            gridSize = 260,
            numContours = 24,
            sigma = 0.5,
            colorMap = ColorMap.TURBO
        )
    }
}
