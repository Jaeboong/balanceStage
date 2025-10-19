package plot.geom

/** 점 좌표들과 평균 평면 사이의 거리(절대/부호)를 계산하는 기능 */
interface DistanceCalculator {
    /** 각 점 p=(x,y,z)의 부호 있는 거리: (n·p + d) (n은 단위벡터 가정) */
    fun signedDistances(points: Array<DoubleArray>, plane: PlaneModel): DoubleArray

    /** 절대 거리 반환 */
    fun distances(points: Array<DoubleArray>, plane: PlaneModel): DoubleArray =
        signedDistances(points, plane).map { kotlin.math.abs(it) }.toDoubleArray()
}
