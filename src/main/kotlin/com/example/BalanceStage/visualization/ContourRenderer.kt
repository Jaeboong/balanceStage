package com.example.BalanceStage.visualization

import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import kotlin.math.*

/**
 * JavaFX Canvas 기반 2D 등고선 렌더링 엔진
 *
 * 3D 측정 포인트의 편차값을 2D 등고선 맵으로 시각화합니다.
 *
 * ## 주요 기능
 * - IDW(Inverse Distance Weighting) 보간을 통한 그리드 생성
 * - 가우시안 스무딩을 통한 노이즈 제거
 * - Marching Squares 알고리즘을 이용한 등고선 추출
 * - 6가지 컬러맵 지원 (TURBO, JET, VIRIDIS, COOLWARM, SPECTRAL, RDYLBU)
 * - 측정점 라벨 표시 (P1, P2, P3...)
 * - 컬러바를 통한 값 범위 표시
 *
 * ## 사용 예제
 * ```kotlin
 * // 1. 측정 데이터 준비 (x, y, deviation)
 * val points = listOf(
 *     Triple(0.0, 0.0, 0.005),
 *     Triple(10.0, 0.0, -0.003),
 *     Triple(10.0, 10.0, 0.002)
 * )
 *
 * // 2. 렌더러 생성
 * val renderer = ContourRenderer(width = 520.0, height = 420.0)
 *
 * // 3. 렌더링
 * val canvas = renderer.render(points, ContourConfig.HIGH_QUALITY)
 *
 * // 4. UI에 추가
 * myPane.children.add(canvas)
 * ```
 *
 * @property width Canvas 너비 (픽셀)
 * @property height Canvas 높이 (픽셀)
 */
class ContourRenderer(
    private val width: Double = 520.0,
    private val height: Double = 420.0
) {

    /**
     * 등고선 렌더링 메인 함수
     *
     * 새로운 Canvas 객체를 생성하고 등고선을 렌더링합니다.
     *
     * @param points 측정 포인트 리스트. 각 요소는 Triple(x좌표, y좌표, z편차값)
     * @param config 렌더링 설정 (기본값: ContourConfig())
     * @return 렌더링된 Canvas 객체
     */
    fun render(
        points: List<Triple<Double, Double, Double>>,
        config: ContourConfig = ContourConfig()
    ): Canvas {
        val canvas = Canvas(width, height)
        val gc = canvas.graphicsContext2D
        renderToGraphicsContext(gc, points, config)
        return canvas
    }

    /**
     * 기존 GraphicsContext에 렌더링
     *
     * Canvas 재사용이 필요한 경우 이 메서드를 사용합니다.
     * 기존 Canvas의 GraphicsContext에 직접 그립니다.
     *
     * @param gc 렌더링할 GraphicsContext
     * @param points 측정 포인트 리스트. 각 요소는 Triple(x좌표, y좌표, z편차값)
     * @param config 렌더링 설정 (기본값: ContourConfig())
     */
    fun renderToGraphicsContext(
        gc: GraphicsContext,
        points: List<Triple<Double, Double, Double>>,
        config: ContourConfig = ContourConfig()
    ) {
        if (points.isEmpty()) {
            // 빈 캔버스
            gc.fill = Color.LIGHTGRAY
            gc.fillRect(0.0, 0.0, width, height)
            gc.fill = Color.BLACK
            gc.fillText("데이터 없음", width / 2 - 30, height / 2)
            return
        }

        // 1. Canvas 내부 여백 설정 (픽셀 단위)
        val canvasPadding = 30.0  // 상하좌우 30px 여백
        val renderWidth = width - 2 * canvasPadding
        val renderHeight = height - 2 * canvasPadding

        // 2. 데이터 범위 계산
        val xMin = points.minOf { it.first }
        val xMax = points.maxOf { it.first }
        val yMin = points.minOf { it.second }
        val yMax = points.maxOf { it.second }
        val zMin = points.minOf { it.third }
        val zMax = points.maxOf { it.third }

        // 3. 데이터의 실제 범위 계산 (여백 10% 추가)
        val dataPadding = 1.1 // 10% 여유
        val xRange = ((xMax - xMin) * dataPadding).coerceAtLeast(1.0)
        val yRange = ((yMax - yMin) * dataPadding).coerceAtLeast(1.0)

        // 4. 중심점 계산
        val xCenter = (xMin + xMax) / 2.0
        val yCenter = (yMin + yMax) / 2.0

        // 5. 렌더링 영역 종횡비에 맞춰 범위 조정
        val renderAspectRatio = renderWidth / renderHeight
        val dataAspectRatio = xRange / yRange

        val (finalXRange, finalYRange) = if (dataAspectRatio > renderAspectRatio) {
            // 데이터가 가로로 더 넓음 -> Y 범위를 확장
            xRange to xRange / renderAspectRatio
        } else {
            // 데이터가 세로로 더 높음 -> X 범위를 확장
            yRange * renderAspectRatio to yRange
        }

        // 6. 뷰포트 범위 계산 (중심점 기준 대칭)
        val xStart = xCenter - finalXRange / 2.0
        val xEnd = xCenter + finalXRange / 2.0
        val yStart = yCenter - finalYRange / 2.0
        val yEnd = yCenter + finalYRange / 2.0

        // 디버깅 출력
        println("등고선 렌더링 범위:")
        println("  Canvas: %.0fx%.0f, 렌더링: %.0fx%.0f, 여백: %.0fpx".format(
            width, height, renderWidth, renderHeight, canvasPadding
        ))
        println("  데이터: X[%.2f, %.2f] Y[%.2f, %.2f]".format(xMin, xMax, yMin, yMax))
        println("  뷰포트: X[%.2f, %.2f] Y[%.2f, %.2f]".format(xStart, xEnd, yStart, yEnd))
        println("  종횡비: Render=%.2f, Data=%.2f".format(renderAspectRatio, dataAspectRatio))

        // 7. 그리드 생성 및 보간
        val grid = createInterpolatedGrid(
            points, xStart, xEnd, yStart, yEnd,
            config.gridSize, config.sigma
        )

        // 8. 그리드 데이터 기준 z 범위 재계산
        val gridZMin = grid.flatMap { it.toList() }.minOrNull() ?: zMin
        val gridZMax = grid.flatMap { it.toList() }.maxOrNull() ?: zMax
        val colorScaleFactor = computeNeutralScaleFactor(gridZMin, gridZMax, config.neutralDeviationThreshold)

        // 9. Canvas 변환 적용 (여백 만큼 이동)
        gc.save()
        gc.translate(canvasPadding, canvasPadding)

        // 10. 컬러맵으로 그리드 렌더링 (렌더링 영역 크기로)
        renderGridWithColorMap(
            gc,
            grid,
            gridZMin,
            gridZMax,
            config.colorMap,
            renderWidth,
            renderHeight,
            colorScaleFactor
        )

        // 11. 등고선 그리기
        drawContourLines(gc, grid, gridZMin, gridZMax, config.numContours, renderWidth, renderHeight)

        // 12. 원본 측정점 표시
        drawOriginalPoints(gc, points, xStart, xEnd, yStart, yEnd, renderWidth, renderHeight)

        // 13. Canvas 변환 복원
        gc.restore()

        // 14. 컬러바 추가 (원래 Canvas 좌표계에서)
        drawColorBar(gc, gridZMin, gridZMax, config.colorMap, colorScaleFactor)
    }

    /**
     * IDW(Inverse Distance Weighting) + 가우시안 스무딩으로 그리드 보간
     */
    private fun createInterpolatedGrid(
        points: List<Triple<Double, Double, Double>>,
        xStart: Double, xEnd: Double,
        yStart: Double, yEnd: Double,
        gridSize: Int,
        sigma: Double
    ): Array<DoubleArray> {
        val grid = Array(gridSize) { DoubleArray(gridSize) }
        val xStep = (xEnd - xStart) / (gridSize - 1)
        val yStep = (yEnd - yStart) / (gridSize - 1)

        // IDW 보간 (안정성 개선)
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                val gx = xStart + i * xStep
                val gy = yStart + j * yStep

                var sumWeights = 0.0
                var sumValues = 0.0
                var exactMatch = false

                points.forEach { (px, py, pz) ->
                    val dist = sqrt((gx - px).pow(2) + (gy - py).pow(2))

                    // 측정점에 정확히 매칭되면 원값 사용
                    if (dist < 0.001) {
                        grid[i][j] = pz
                        exactMatch = true
                        return@forEach  // 이 그리드 셀은 완료
                    }

                    val weight = 1.0 / (dist.pow(2) + 1e-6)
                    sumWeights += weight
                    sumValues += weight * pz
                }

                // 측정점이 아닌 경우에만 가중 평균 계산
                if (!exactMatch) {
                    grid[i][j] = if (sumWeights > 0) sumValues / sumWeights else 0.0
                }
            }
        }

        // 가우시안 스무딩 (선택적)
        if (sigma > 0.01) {
            return applyGaussianSmoothing(grid, sigma)
        }

        return grid
    }

    /**
     * 간단한 가우시안 스무딩 (3x3 커널)
     */
    private fun applyGaussianSmoothing(grid: Array<DoubleArray>, sigma: Double): Array<DoubleArray> {
        val size = grid.size
        val smoothed = Array(size) { DoubleArray(size) }
        val kernel = gaussianKernel3x3(sigma)

        for (i in 1 until size - 1) {
            for (j in 1 until size - 1) {
                var sum = 0.0
                for (di in -1..1) {
                    for (dj in -1..1) {
                        sum += grid[i + di][j + dj] * kernel[di + 1][dj + 1]
                    }
                }
                smoothed[i][j] = sum
            }
        }

        // 경계 복사
        for (i in 0 until size) {
            smoothed[i][0] = grid[i][0]
            smoothed[i][size - 1] = grid[i][size - 1]
            smoothed[0][i] = grid[0][i]
            smoothed[size - 1][i] = grid[size - 1][i]
        }

        return smoothed
    }

    private fun gaussianKernel3x3(sigma: Double): Array<DoubleArray> {
        val kernel = Array(3) { DoubleArray(3) }
        var sum = 0.0
        for (i in -1..1) {
            for (j in -1..1) {
                val value = exp(-(i * i + j * j) / (2 * sigma * sigma))
                kernel[i + 1][j + 1] = value
                sum += value
            }
        }
        // 정규화
        for (i in 0..2) {
            for (j in 0..2) {
                kernel[i][j] /= sum
            }
        }
        return kernel
    }

    /**
     * 컬러맵으로 그리드 렌더링
     */
    private fun renderGridWithColorMap(
        gc: GraphicsContext,
        grid: Array<DoubleArray>,
        zMin: Double,
        zMax: Double,
        colorMap: ColorMap,
        renderWidth: Double,
        renderHeight: Double,
        scaleFactor: Double
    ) {
        val size = grid.size
        val cellWidth = renderWidth / size
        val cellHeight = renderHeight / size
        val zRange = (zMax - zMin).coerceAtLeast(0.001)

        for (i in 0 until size) {
            for (j in 0 until size) {
                val normalized = ((grid[i][j] - zMin) / zRange).coerceIn(0.0, 1.0)
                val adjusted = applyNeutralCompression(normalized, scaleFactor)
                val color = getColor(adjusted, colorMap)

                gc.fill = color
                gc.fillRect(i * cellWidth, (size - 1 - j) * cellHeight, cellWidth + 1, cellHeight + 1)
            }
        }
    }

    /**
     * 등고선 그리기 (Marching Squares 알고리즘)
     */
    private fun drawContourLines(
        gc: GraphicsContext,
        grid: Array<DoubleArray>,
        zMin: Double,
        zMax: Double,
        numContours: Int,
        renderWidth: Double,
        renderHeight: Double
    ) {
        val size = grid.size
        val cellWidth = renderWidth / size
        val cellHeight = renderHeight / size
        val zRange = (zMax - zMin).coerceAtLeast(0.001)

        gc.stroke = Color.BLACK
        gc.lineWidth = 1.0

        // 등고선 레벨 계산
        val levels = (1 until numContours).map { i ->
            zMin + (zRange * i / numContours)
        }

        levels.forEach { level ->
            for (i in 0 until size - 1) {
                for (j in 0 until size - 1) {
                    val v00 = grid[i][j]
                    val v10 = grid[i + 1][j]
                    val v01 = grid[i][j + 1]
                    val v11 = grid[i + 1][j + 1]

                    // Marching Squares: 선분 그리기
                    drawMarchingSquareSegment(
                        gc, v00, v10, v01, v11, level,
                        i * cellWidth, (size - 1 - j) * cellHeight,
                        cellWidth, cellHeight
                    )
                }
            }
        }
    }

    /**
     * Marching Squares 셀 내 선분 그리기
     */
    private fun drawMarchingSquareSegment(
        gc: GraphicsContext,
        v00: Double, v10: Double, v01: Double, v11: Double,
        level: Double,
        x: Double, y: Double,
        w: Double, h: Double
    ) {
        // 4개 코너가 레벨보다 큰지 판정
        val case = (if (v00 >= level) 1 else 0) or
                (if (v10 >= level) 2 else 0) or
                (if (v11 >= level) 4 else 0) or
                (if (v01 >= level) 8 else 0)

        // 선형 보간으로 교점 계산
        fun lerp(a: Double, b: Double, va: Double, vb: Double): Double {
            val t = (level - va) / (vb - va + 1e-10)
            return a + t * (b - a)
        }

        val left = lerp(y + h, y, v00, v01)
        val right = lerp(y + h, y, v10, v11)
        val bottom = lerp(x, x + w, v00, v10)
        val top = lerp(x, x + w, v01, v11)

        // 16가지 케이스 처리 (간소화)
        when (case) {
            1, 14 -> gc.strokeLine(x, left, bottom, y + h)
            2, 13 -> gc.strokeLine(bottom, y + h, x + w, right)
            3, 12 -> gc.strokeLine(x, left, x + w, right)
            4, 11 -> gc.strokeLine(x + w, right, top, y)
            5 -> {
                gc.strokeLine(x, left, top, y)
                gc.strokeLine(bottom, y + h, x + w, right)
            }
            6, 9 -> gc.strokeLine(bottom, y + h, top, y)
            7, 8 -> gc.strokeLine(x, left, top, y)
            10 -> {
                gc.strokeLine(x, left, bottom, y + h)
                gc.strokeLine(top, y, x + w, right)
            }
        }
    }

    /**
     * 원본 측정점 표시 (텍스트 라벨로)
     */
    private fun drawOriginalPoints(
        gc: GraphicsContext,
        points: List<Triple<Double, Double, Double>>,
        xStart: Double, xEnd: Double,
        yStart: Double, yEnd: Double,
        renderWidth: Double,
        renderHeight: Double
    ) {
        val xRange = xEnd - xStart
        val yRange = yEnd - yStart

        gc.font = javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 12.0)
        gc.stroke = Color.WHITE
        gc.lineWidth = 2.5

        points.forEachIndexed { index, (px, py, pz) ->
            val sx = ((px - xStart) / xRange * renderWidth).coerceIn(0.0, renderWidth)
            val sy = renderHeight - ((py - yStart) / yRange * renderHeight).coerceIn(0.0, renderHeight)

            // 점 라벨 (P1, P2, P3...)
            val label = "P${index + 1}"

            // 흰색 외곽선
            gc.strokeText(label, sx - 8, sy - 8)

            // 검은색 텍스트
            gc.fill = Color.BLACK
            gc.fillText(label, sx - 8, sy - 8)
        }
    }

    /**
     * 컬러바 추가
     */
    private fun drawColorBar(
        gc: GraphicsContext,
        zMin: Double,
        zMax: Double,
        colorMap: ColorMap,
        scaleFactor: Double
    ) {
        val barWidth = 20.0
        val barHeight = height * 0.6
        val barX = width - barWidth - 15
        val barY = (height - barHeight) / 2

        // 컬러바 그리기
        val steps = 100
        val stepHeight = barHeight / steps

        for (i in 0 until steps) {
            val t = i.toDouble() / steps
            val adjusted = applyNeutralCompression(t, scaleFactor)
            val color = getColor(adjusted, colorMap)
            gc.fill = color
            gc.fillRect(barX, barY + barHeight - (i + 1) * stepHeight, barWidth, stepHeight + 1)
        }

        // 테두리
        gc.stroke = Color.BLACK
        gc.lineWidth = 1.0
        gc.strokeRect(barX, barY, barWidth, barHeight)

        // 레이블
        gc.fill = Color.BLACK
        gc.fillText(String.format("%.3f", zMax), barX + barWidth + 5, barY + 5)
        gc.fillText(String.format("%.3f", zMin), barX + barWidth + 5, barY + barHeight)
    }

    /**
     * 컬러맵 함수
     */
    private fun getColor(t: Double, colorMap: ColorMap): Color {
        return when (colorMap) {
            ColorMap.TURBO -> turboColorMap(t)
            ColorMap.JET -> jetColorMap(t)
            ColorMap.VIRIDIS -> viridisColorMap(t)
            ColorMap.COOLWARM -> coolwarmColorMap(t)
            ColorMap.SPECTRAL -> spectralColorMap(t)
            ColorMap.RDYLBU -> rdylbuColorMap(t)
        }
    }

    private fun computeNeutralScaleFactor(zMin: Double, zMax: Double, threshold: Double): Double {
        if (threshold <= 0.0) return 1.0
        val maxAbs = max(abs(zMin), abs(zMax))
        return (maxAbs / threshold).coerceIn(0.0, 1.0)
    }

    private fun applyNeutralCompression(normalized: Double, scaleFactor: Double): Double {
        if (scaleFactor >= 0.999) return normalized
        return ((normalized - 0.5) * scaleFactor + 0.5).coerceIn(0.0, 1.0)
    }

    private fun turboColorMap(t: Double): Color {
        // Turbo colormap approximation
        val r = (0.13 + 2.04 * t - 4.30 * t.pow(2) + 2.92 * t.pow(3)).coerceIn(0.0, 1.0)
        val g = (0.09 + 2.28 * t - 3.82 * t.pow(2) + 2.45 * t.pow(3)).coerceIn(0.0, 1.0)
        val b = (0.56 + 1.47 * t - 2.37 * t.pow(2) + 1.13 * t.pow(3)).coerceIn(0.0, 1.0)
        return Color.color(r, g, b)
    }

    private fun jetColorMap(t: Double): Color {
        val r = (1.5 - abs(4 * t - 3)).coerceIn(0.0, 1.0)
        val g = (1.5 - abs(4 * t - 2)).coerceIn(0.0, 1.0)
        val b = (1.5 - abs(4 * t - 1)).coerceIn(0.0, 1.0)
        return Color.color(r, g, b)
    }

    private fun viridisColorMap(t: Double): Color {
        // Simplified viridis approximation
        val r = (0.28 + 0.78 * t - 0.67 * t.pow(2)).coerceIn(0.0, 1.0)
        val g = (0.00 + 0.97 * t).coerceIn(0.0, 1.0)
        val b = (0.33 + 0.11 * t + 0.38 * t.pow(2)).coerceIn(0.0, 1.0)
        return Color.color(r, g, b)
    }

    private fun coolwarmColorMap(t: Double): Color {
        val r = t
        val g = 0.5
        val b = 1.0 - t
        return Color.color(r, g, b)
    }

    private fun spectralColorMap(t: Double): Color {
        // Spectral colormap: 빨강 → 주황 → 노랑 → 초록 → 파랑 → 보라
        // 등고선 지도에서 많이 사용
        return when {
            t < 0.2 -> {
                // 빨강 → 주황
                val s = t / 0.2
                Color.color(1.0, s * 0.5, 0.0)
            }
            t < 0.4 -> {
                // 주황 → 노랑
                val s = (t - 0.2) / 0.2
                Color.color(1.0, 0.5 + s * 0.5, 0.0)
            }
            t < 0.6 -> {
                // 노랑 → 초록
                val s = (t - 0.4) / 0.2
                Color.color(1.0 - s, 1.0, 0.0)
            }
            t < 0.8 -> {
                // 초록 → 파랑
                val s = (t - 0.6) / 0.2
                Color.color(0.0, 1.0 - s * 0.5, s)
            }
            else -> {
                // 파랑 → 보라
                val s = (t - 0.8) / 0.2
                Color.color(s * 0.5, 0.5 - s * 0.5, 1.0)
            }
        }
    }

    private fun rdylbuColorMap(t: Double): Color {
        // RdYlBu: 빨강 → 노랑 → 파랑 (기상도/지형도 스타일)
        return when {
            t < 0.25 -> {
                // 진한 빨강 → 빨강
                val s = t / 0.25
                Color.color(0.5 + s * 0.5, 0.0, 0.0)
            }
            t < 0.5 -> {
                // 빨강 → 노랑
                val s = (t - 0.25) / 0.25
                Color.color(1.0, s, 0.0)
            }
            t < 0.75 -> {
                // 노랑 → 연한 파랑
                val s = (t - 0.5) / 0.25
                Color.color(1.0 - s, 1.0 - s * 0.3, s * 0.7)
            }
            else -> {
                // 연한 파랑 → 진한 파랑
                val s = (t - 0.75) / 0.25
                Color.color(0.0, 0.7 - s * 0.7, 0.7 + s * 0.3)
            }
        }
    }
}
