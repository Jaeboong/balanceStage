package com.example.BalanceStage.service.impl

import com.example.BalanceStage.service.InterpolatedGrid
import com.example.BalanceStage.service.RbfInterpolationService
import org.springframework.stereotype.Service
import kotlin.math.*

/**
 * 가우시안 RBF 보간 구현
 * BalanceSurfaceApp의 알고리즘 사용
 */
@Service
class GaussianRbfInterpolationService : RbfInterpolationService {

    override fun interpolate(
        points: List<Triple<Double, Double, Double>>,
        gridResolution: Int
    ): InterpolatedGrid {
        require(points.size >= 3) { "RBF 보간에는 최소 3개의 점이 필요합니다" }

        val x = points.map { it.first }.toDoubleArray()
        val y = points.map { it.second }.toDoubleArray()
        val z = points.map { it.third }.toDoubleArray()

        // RBF 파라미터
        val sigmaScale = 0.50
        val lambda = 1e-9

        // 그리드 생성
        val (xi, yi) = computeGrid(x, y, gridResolution, centerOrigin = false, pad = 0.1)
        val (Xg, Yg) = meshgrid(xi, yi)

        // RBF 보간
        val sigma = pickSigmaFromPoints(x, y, sigmaScale)
        val w = rbfGaussianWeights(x, y, z, sigma, lambda)
        val Zg = rbfGaussianEval(w, x, y, Xg, Yg, sigma)

        // Min/Max 계산
        var minZ = Zg[0][0]
        var maxZ = Zg[0][0]
        for (row in Zg) {
            for (value in row) {
                if (value < minZ) minZ = value
                if (value > maxZ) maxZ = value
            }
        }

        return InterpolatedGrid(xi, yi, Zg, minZ, maxZ)
    }

    // ========== RBF 알고리즘 (BalanceSurfaceApp에서 복사) ==========

    private fun pickSigmaFromPoints(x: DoubleArray, y: DoubleArray, scale: Double): Double {
        var dmin = Double.POSITIVE_INFINITY
        for (i in x.indices) {
            for (j in i + 1 until x.size) {
                dmin = min(dmin, hypot(x[i] - x[j], y[i] - y[j]))
            }
        }
        return max(dmin * scale, 1e-6)
    }

    private fun rbfGaussianWeights(
        x: DoubleArray, y: DoubleArray, v: DoubleArray,
        sigma: Double, lam: Double
    ): DoubleArray {
        val n = x.size
        val K = Array(n) { DoubleArray(n) }
        val s2 = sigma * sigma

        for (i in 0 until n) {
            for (j in 0 until n) {
                val d2 = (x[i] - x[j]).pow(2) + (y[i] - y[j]).pow(2)
                K[i][j] = exp(-d2 / s2)
            }
            K[i][i] += lam
        }

        return solve(K, v)
    }

    private fun rbfGaussianEval(
        w: DoubleArray, xPts: DoubleArray, yPts: DoubleArray,
        Xg: Array<DoubleArray>, Yg: Array<DoubleArray>, sigma: Double
    ): Array<DoubleArray> {
        val rows = Xg.size
        val cols = Xg[0].size
        val Z = Array(rows) { DoubleArray(cols) }
        val s2 = sigma * sigma

        for (j in w.indices) {
            val xj = xPts[j]
            val yj = yPts[j]
            val wj = w[j]
            for (r in 0 until rows) {
                val Xr = Xg[r]
                val Yr = Yg[r]
                val Zr = Z[r]
                for (c in 0 until cols) {
                    val d2 = (Xr[c] - xj).pow(2) + (Yr[c] - yj).pow(2)
                    Zr[c] += wj * exp(-d2 / s2)
                }
            }
        }
        return Z
    }

    private fun solve(Ain: Array<DoubleArray>, bin: DoubleArray): DoubleArray {
        val n = bin.size
        val A = Array(n) { Ain[it].clone() }
        val b = bin.clone()

        for (p in 0 until n) {
            // Pivot
            var maxI = p
            for (i in p + 1 until n) {
                if (abs(A[i][p]) > abs(A[maxI][p])) maxI = i
            }
            val tmp = A[p]; A[p] = A[maxI]; A[maxI] = tmp
            val tb = b[p]; b[p] = b[maxI]; b[maxI] = tb

            val piv = A[p][p]
            if (abs(piv) < 1e-12) continue

            for (j in p until n) A[p][j] /= piv
            b[p] /= piv

            for (i in 0 until n) {
                if (i != p) {
                    val a = A[i][p]
                    if (a != 0.0) {
                        for (j in p until n) A[i][j] -= a * A[p][j]
                        b[i] -= a * b[p]
                    }
                }
            }
        }
        return b
    }

    private fun computeGrid(
        x: DoubleArray, y: DoubleArray, res: Int, centerOrigin: Boolean, pad: Double
    ): Pair<DoubleArray, DoubleArray> {
        val xmin = x.minOrNull() ?: -1.0
        val xmax = x.maxOrNull() ?: 1.0
        val ymin = y.minOrNull() ?: -1.0
        val ymax = y.maxOrNull() ?: 1.0

        val xs: DoubleArray
        val ys: DoubleArray

        if (centerOrigin) {
            val Rx = max(abs(xmin), abs(xmax)) * (1 + pad)
            val Ry = max(abs(ymin), abs(ymax)) * (1 + pad)
            xs = linspace(-Rx, Rx, res)
            ys = linspace(-Ry, Ry, res)
        } else {
            val dx = (xmax - xmin) * pad
            val dy = (ymax - ymin) * pad
            xs = linspace(xmin - dx, xmax + dx, res)
            ys = linspace(ymin - dy, ymax + dy, res)
        }

        return xs to ys
    }

    private fun meshgrid(xi: DoubleArray, yi: DoubleArray): Pair<Array<DoubleArray>, Array<DoubleArray>> {
        val rows = yi.size
        val cols = xi.size
        val X = Array(rows) { DoubleArray(cols) }
        val Y = Array(rows) { DoubleArray(cols) }

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                X[r][c] = xi[c]
                Y[r][c] = yi[r]
            }
        }

        return X to Y
    }

    private fun linspace(start: Double, end: Double, n: Int): DoubleArray {
        if (n <= 1) return doubleArrayOf(start)
        val step = (end - start) / (n - 1)
        return DoubleArray(n) { start + it * step }
    }
}
