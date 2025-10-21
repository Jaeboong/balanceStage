package com.example.BalanceStage.util

import kotlin.math.sqrt
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.EigenDecomposition

/**
 * PCA(주성분분석)를 이용한 평균 평면 계산 유틸리티
 * n개의 3D 점들로부터 최적 평면을 찾고, 각 점의 편차를 계산합니다.
 */
object PCAPlaneCalculator {

    /**
     * 3D 점 데이터 클래스
     */
    data class Point3D(val x: Double, val y: Double, val z: Double)

    /**
     * 평면 정의: ax + by + cz + d = 0 형태
     * 법선 벡터 (a, b, c)와 중심점(centroid)로 표현
     */
    data class Plane(
        val a: Double,
        val b: Double,
        val c: Double,
        val d: Double,
        val centroid: Point3D
    ) {
        /**
         * 점과 평면 사이의 거리(편차) 계산
         */
        fun distanceToPoint(point: Point3D): Double {
            val numerator = a * point.x + b * point.y + c * point.z + d
            val denominator = sqrt(a * a + b * b + c * c)
            return numerator / denominator
        }
    }

    /**
     * PCA를 이용해 점들의 평균 평면 계산
     * @param points 3D 점들의 리스트
     * @return 계산된 평면 또는 null (점이 3개 미만일 경우)
     */
    fun calculateBestFitPlane(points: List<Point3D>): Plane? {
        if (points.size < 3) return null

        // 점이 정확히 3개일 때는 외적으로 평면 계산 (더 정확함)
        if (points.size == 3) {
            return calculatePlaneFrom3Points(points[0], points[1], points[2])
        }

        // 1. 중심점(centroid) 계산
        val centroid = Point3D(
            x = points.map { it.x }.average(),
            y = points.map { it.y }.average(),
            z = points.map { it.z }.average()
        )

        // 2. 중심점 기준으로 정규화된 점들 생성
        val normalizedPoints = points.map { p ->
            Point3D(
                x = p.x - centroid.x,
                y = p.y - centroid.y,
                z = p.z - centroid.z
            )
        }

        // 3. 공분산 행렬 계산 (3x3 대칭 행렬)
        var cxx = 0.0
        var cxy = 0.0
        var cxz = 0.0
        var cyy = 0.0
        var cyz = 0.0
        var czz = 0.0

        normalizedPoints.forEach { p ->
            cxx += p.x * p.x
            cxy += p.x * p.y
            cxz += p.x * p.z
            cyy += p.y * p.y
            cyz += p.y * p.z
            czz += p.z * p.z
        }

        val n = points.size
        cxx /= n
        cxy /= n
        cxz /= n
        cyy /= n
        cyz /= n
        czz /= n

        // 4. 공분산 행렬의 최소 고유벡터 찾기 (평면의 법선 벡터)
        // 간단한 Power Iteration 방법 사용
        val normal = findSmallestEigenvector(cxx, cxy, cxz, cyy, cyz, czz)

        // 5. 평면 방정식: a(x-x0) + b(y-y0) + c(z-z0) = 0
        // 전개하면: ax + by + cz + d = 0, d = -(ax0 + by0 + cz0)
        val d = -(normal.x * centroid.x + normal.y * centroid.y + normal.z * centroid.z)

        return Plane(normal.x, normal.y, normal.z, d, centroid)
    }

    /**
     * 3x3 대칭 행렬의 최소 고유벡터 계산 (평면의 법선 방향)
     * Apache Commons Math의 EigenDecomposition 사용
     */
    private fun findSmallestEigenvector(
        cxx: Double, cxy: Double, cxz: Double,
        cyy: Double, cyz: Double, czz: Double
    ): Point3D {
        // 공분산 행렬 생성
        val covMatrix = Array2DRowRealMatrix(arrayOf(
            doubleArrayOf(cxx, cxy, cxz),
            doubleArrayOf(cxy, cyy, cyz),
            doubleArrayOf(cxz, cyz, czz)
        ))

        // 고유값 분해
        val eigen = EigenDecomposition(covMatrix)

        // 최소 고유값의 인덱스 찾기
        val minIdx = (0..2).minByOrNull { eigen.getRealEigenvalue(it) } ?: 0
        val eigenvector = eigen.getEigenvector(minIdx)

        // Point3D로 변환
        return Point3D(
            eigenvector.getEntry(0),
            eigenvector.getEntry(1),
            eigenvector.getEntry(2)
        )
    }

    /**
     * 3개 점으로 정확한 평면 계산 (외적 사용)
     * 평면 방정식: (P1P2 × P1P3) · (P - P1) = 0
     */
    private fun calculatePlaneFrom3Points(p1: Point3D, p2: Point3D, p3: Point3D): Plane {
        // 두 벡터 생성
        val v1 = Point3D(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z)
        val v2 = Point3D(p3.x - p1.x, p3.y - p1.y, p3.z - p1.z)

        // 외적으로 법선 벡터 계산 (v1 × v2)
        val normal = Point3D(
            x = v1.y * v2.z - v1.z * v2.y,
            y = v1.z * v2.x - v1.x * v2.z,
            z = v1.x * v2.y - v1.y * v2.x
        )

        // 법선 벡터 정규화
        val mag = sqrt(normal.x * normal.x + normal.y * normal.y + normal.z * normal.z)
        val a = normal.x / mag
        val b = normal.y / mag
        val c = normal.z / mag

        // 중심점 (3개 점의 평균)
        val centroid = Point3D(
            x = (p1.x + p2.x + p3.x) / 3.0,
            y = (p1.y + p2.y + p3.y) / 3.0,
            z = (p1.z + p2.z + p3.z) / 3.0
        )

        // d = -(ax0 + by0 + cz0)
        val d = -(a * centroid.x + b * centroid.y + c * centroid.z)

        return Plane(a, b, c, d, centroid)
    }

    /**
     * 점들과 평면의 편차 계산
     * @param points 측정점들
     * @param plane 기준 평면
     * @return 각 점의 (x, y, 편차) 리스트
     */
    fun calculateDeviations(points: List<Point3D>, plane: Plane): List<Triple<Double, Double, Double>> {
        return points.map { point ->
            val deviation = plane.distanceToPoint(point)
            Triple(point.x, point.y, deviation)
        }
    }
}
