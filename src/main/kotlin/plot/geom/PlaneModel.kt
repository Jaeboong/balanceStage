package plot.geom

/** z = -(nx*x + ny*y + d)/nz  형태.
 *  normal 은 단위벡터(정규화)로 가정한다.
 */
data class PlaneModel(
    val normal: DoubleArray,  // [nx, ny, nz]
    val d: Double,            // 평면 상수항
    val centroid: DoubleArray // [cx, cy, cz]
)
