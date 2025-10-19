package plot.geom

/** (x[], y[], z[]) 가 들어오면 평균평면을 추정하는 기능 */
interface PlaneEstimator {
    fun estimate(x: DoubleArray, y: DoubleArray, z: DoubleArray): PlaneModel

    /** 주어진 X,Y 그리드에서 평면의 z 값을 계산 (z = -(nx x + ny y + d)/nz) */
    fun planeZ(normal: DoubleArray, d: Double, X: Array<DoubleArray>, Y: Array<DoubleArray>): Array<DoubleArray>
}
