package plot.geom

/** 현재 평면이 단위법선(normalized normal)을 갖는다는 가정하에, 거리 = n·p + d */
class DefaultDistanceCalculator : DistanceCalculator {
    override fun signedDistances(points: Array<DoubleArray>, plane: PlaneModel): DoubleArray {
        val (nx, ny, nz) = plane.normal
        val d = plane.d
        val out = DoubleArray(points.size)
        for (i in points.indices) {
            val px = points[i][0]; val py = points[i][1]; val pz = points[i][2]
            out[i] = nx*px + ny*py + nz*pz + d
        }
        return out
    }
}
