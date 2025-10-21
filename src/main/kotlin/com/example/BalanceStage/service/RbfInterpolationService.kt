package com.example.BalanceStage.service

/**
 * RBF (Radial Basis Function) 보간 서비스
 * 희소한 점 데이터로부터 조밀한 그리드 생성
 */
interface RbfInterpolationService {
    /**
     * RBF 가우시안 보간으로 그리드 생성
     * @param points 입력 점들 [(x, y, z), ...]
     * @param gridResolution 그리드 해상도 (한 변의 점 개수)
     * @return InterpolatedGrid 객체
     */
    fun interpolate(
        points: List<Triple<Double, Double, Double>>,
        gridResolution: Int = 100
    ): InterpolatedGrid
}

/**
 * 보간된 그리드 데이터
 */
data class InterpolatedGrid(
    val xGrid: DoubleArray,           // X 좌표 배열
    val yGrid: DoubleArray,           // Y 좌표 배열
    val zGrid: Array<DoubleArray>,    // Z 값 2D 배열 [rows][cols]
    val minZ: Double,
    val maxZ: Double
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as InterpolatedGrid
        return xGrid.contentEquals(other.xGrid) &&
               yGrid.contentEquals(other.yGrid) &&
               zGrid.contentDeepEquals(other.zGrid)
    }

    override fun hashCode(): Int {
        var result = xGrid.contentHashCode()
        result = 31 * result + yGrid.contentHashCode()
        result = 31 * result + zGrid.contentDeepHashCode()
        return result
    }
}
