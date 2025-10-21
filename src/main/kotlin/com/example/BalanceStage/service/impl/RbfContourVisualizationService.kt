package com.example.BalanceStage.service.impl

import com.example.BalanceStage.service.ColorMappingService
import com.example.BalanceStage.service.ContourVisualizationService
import com.example.BalanceStage.service.RbfInterpolationService
import javafx.scene.*
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.Box
import javafx.scene.shape.CullFace
import javafx.scene.shape.MeshView
import javafx.scene.shape.TriangleMesh
import javafx.scene.transform.Rotate
import org.springframework.stereotype.Service
import kotlin.math.max
import kotlin.math.min

/**
 * RBF 보간 기반 3D 등고선 시각화
 * Image #2와 같은 부드러운 곡면 + 등고선 표시
 */
@Service("rbfContourVisualization")
class RbfContourVisualizationService(
    private val rbfInterpolation: RbfInterpolationService,
    private val colorMapper: ColorMappingService
) : ContourVisualizationService {

    override fun createContourView(
        points: List<Triple<Double, Double, Double>>,
        width: Double,
        height: Double
    ): SubScene {
        if (points.size < 3) {
            throw IllegalArgumentException("최소 3개 이상의 포인트가 필요합니다")
        }

        println("✓ RBF 등고선 시각화 시작 (${points.size}개 포인트)")

        // Step 1: RBF 보간으로 그리드 생성 (Z값으로 보간)
        val grid = rbfInterpolation.interpolate(points, gridResolution = 80)
        println("  RBF 그리드 생성 완료: ${grid.xGrid.size} x ${grid.yGrid.size}")
        println("  Z 범위: [${String.format("%.3f", grid.minZ)}, ${String.format("%.3f", grid.maxZ)}]")

        // Step 2: 스케일 계산
        val xRange = grid.xGrid.last() - grid.xGrid.first()
        val yRange = grid.yGrid.last() - grid.yGrid.first()
        val zRange = grid.maxZ - grid.minZ

        val zScale = 50.0  // Z 확대 배율 (높이 차이를 명확하게)
        val xyScale = 800.0 / max(xRange, yRange)

        // Step 3: Create 3D world
        val world = Group()

        // Add lighting
        addLighting(world)

        // Step 4: 보간된 곡면 메쉬 생성
        val surfaceMesh = createSurfaceMesh(grid, xyScale, zScale)
        world.children.add(surfaceMesh)

        // Step 5: 등고선 추가
        val contourLines = createContourLines(grid, xyScale, zScale, numLevels = 15)
        world.children.addAll(contourLines)

        // Step 6: 원본 포인트 마커
        val xCenter = (grid.xGrid.first() + grid.xGrid.last()) / 2.0
        val yCenter = (grid.yGrid.first() + grid.yGrid.last()) / 2.0
        val zCenter = (grid.minZ + grid.maxZ) / 2.0

        points.forEach { (x, y, z) ->
            val scaledX = (x - xCenter) * xyScale
            val scaledY = (y - yCenter) * xyScale
            val scaledZ = (z - zCenter) * zScale

            val marker = javafx.scene.shape.Sphere(6.0).apply {
                material = PhongMaterial(Color.BLACK)
                translateX = scaledX
                translateY = scaledY
                translateZ = scaledZ + 2.0  // Slightly above surface
            }
            world.children.add(marker)
        }

        // Step 7: 좌표축
        addAxes(world, max(xRange, yRange) * xyScale * 0.5)

        // DEBUG: 원점에 큰 구체 추가
        val debugSphere = javafx.scene.shape.Sphere(30.0).apply {
            material = PhongMaterial(Color.RED)
            translateX = 0.0
            translateY = 0.0
            translateZ = 0.0
        }
        world.children.add(debugSphere)
        println("  [DEBUG] 원점에 빨간 구체 추가 (반지름 30)")

        // Step 8: 카메라 설정 - 오른쪽 상단에서 중앙을 향해
        val sceneSize = max(xRange, yRange) * xyScale
        val viewDistance = sceneSize * 0.5  // 적당한 거리

        println("  씬 크기: ${String.format("%.1f", sceneSize)}")
        println("  카메라 거리: ${String.format("%.1f", viewDistance)}")

        val camera = PerspectiveCamera(true).apply {
            nearClip = 1.0
            farClip = 20000.0
            fieldOfView = 60.0
        }

        // 카메라를 오른쪽(+X) 상단(+Y) 뒤쪽(-Z)에 배치
        camera.translateX = viewDistance * -0.6   // 오른쪽
        camera.translateY = -viewDistance * -1.1  // 위쪽 (Y는 반대)
        camera.translateZ = -viewDistance * 1.0       // 뒤쪽

        // 중앙(0,0,0)을 향하도록 회전
        camera.transforms.addAll(
            Rotate(20.0, Rotate.X_AXIS),   // 아래를 봄
            Rotate(40.0, Rotate.Y_AXIS),    // 왼쪽을 봄
            Rotate(0.0, Rotate.Z_AXIS)
        )

        println("  카메라 위치: (${camera.translateX}, ${camera.translateY}, ${camera.translateZ})")
        println("  카메라 회전: X=-35°, Y=30°")

        println("✓ RBF 등고선 시각화 완료")
        println("  World 자식 수: ${world.children.size}")

        return SubScene(world, width, height, true, SceneAntialiasing.BALANCED).apply {
            fill = Color.DARKGRAY  // 디버그용 회색 배경
            this.camera = camera
            depthTest = DepthTest.ENABLE
        }
    }

    /**
     * RBF 보간된 곡면 메쉬 생성
     * 중요: X, Y는 평면에 수평으로 배치하고, Z는 높이로 사용
     */
    private fun createSurfaceMesh(
        grid: com.example.BalanceStage.service.InterpolatedGrid,
        xyScale: Double,
        zScale: Double
    ): MeshView {
        val rows = grid.yGrid.size
        val cols = grid.xGrid.size

        val mesh = TriangleMesh()

        val xCenter = (grid.xGrid.first() + grid.xGrid.last()) / 2.0
        val yCenter = (grid.yGrid.first() + grid.yGrid.last()) / 2.0
        val zCenter = (grid.minZ + grid.maxZ) / 2.0

        println("  메쉬 생성: ${cols}x${rows} 버텍스")

        // Vertices - X,Y는 수평 평면, Z는 위쪽 높이
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = (grid.xGrid[c] - xCenter) * xyScale
                val y = (grid.yGrid[r] - yCenter) * xyScale
                val z = (grid.zGrid[r][c] - zCenter) * zScale  // Z가 위쪽

                mesh.points.addAll(x.toFloat(), y.toFloat(), z.toFloat())
            }
        }

        // Texture coordinates (for color mapping)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val u = c.toFloat() / (cols - 1).toFloat()
                val v = r.toFloat() / (rows - 1).toFloat()
                mesh.texCoords.addAll(u, v)
            }
        }

        // Faces
        fun vid(r: Int, c: Int) = r * cols + c
        for (r in 0 until rows - 1) {
            for (c in 0 until cols - 1) {
                val v00 = vid(r, c)
                val v01 = vid(r, c + 1)
                val v10 = vid(r + 1, c)
                val v11 = vid(r + 1, c + 1)

                mesh.faces.addAll(v00, v00, v10, v10, v11, v11)
                mesh.faces.addAll(v00, v00, v11, v11, v01, v01)
            }
        }

        return MeshView(mesh).apply {
            material = PhongMaterial(Color.color(0.7, 0.8, 0.9, 0.7))  // Semi-transparent blue
            cullFace = CullFace.NONE
        }
    }

    /**
     * 등고선 생성 (Marching Squares 알고리즘 간소화)
     */
    private fun createContourLines(
        grid: com.example.BalanceStage.service.InterpolatedGrid,
        xyScale: Double,
        zScale: Double,
        numLevels: Int
    ): List<javafx.scene.Node> {
        val lines = mutableListOf<javafx.scene.Node>()

        val xCenter = (grid.xGrid.first() + grid.xGrid.last()) / 2.0
        val yCenter = (grid.yGrid.first() + grid.yGrid.last()) / 2.0
        val zCenter = (grid.minZ + grid.maxZ) / 2.0

        // 등고선 레벨 계산
        val levels = (0 until numLevels).map { i ->
            grid.minZ + (grid.maxZ - grid.minZ) * i / (numLevels - 1)
        }

        levels.forEach { level ->
            // 색상 매핑
            val zNormalized = ((level - grid.minZ) / (grid.maxZ - grid.minZ) - 0.5) * 2.0
            val color = colorMapper.mapDeviationToColor(zNormalized, 1.0)

            // 간단한 등고선 추출: 그리드 셀의 모서리를 검사
            val rows = grid.yGrid.size
            val cols = grid.xGrid.size

            for (r in 0 until rows - 1) {
                for (c in 0 until cols - 1) {
                    // 셀의 4개 코너 값
                    val z00 = grid.zGrid[r][c]
                    val z01 = grid.zGrid[r][c + 1]
                    val z10 = grid.zGrid[r + 1][c]
                    val z11 = grid.zGrid[r + 1][c + 1]

                    // 레벨이 셀을 통과하는지 확인
                    val minZ = min(min(z00, z01), min(z10, z11))
                    val maxZ = max(max(z00, z01), max(z10, z11))

                    if (level in minZ..maxZ) {
                        // 등고선이 이 셀을 통과함
                        // 간단히 셀 중심에 작은 박스로 표시
                        val xMid = (grid.xGrid[c] + grid.xGrid[c + 1]) / 2.0
                        val yMid = (grid.yGrid[r] + grid.yGrid[r + 1]) / 2.0

                        val x = (xMid - xCenter) * xyScale
                        val y = (yMid - yCenter) * xyScale
                        val z = (level - zCenter) * zScale

                        val box = Box(1.5, 1.5, 0.5).apply {
                            material = PhongMaterial(color)
                            translateX = x
                            translateY = y
                            translateZ = z
                        }
                        lines.add(box)
                    }
                }
            }
        }

        return lines
    }

    private fun addLighting(world: Group) {
        val ambientLight = AmbientLight(Color.color(0.7, 0.7, 0.7))
        val pointLight1 = PointLight(Color.WHITE).apply {
            translateX = 200.0
            translateY = -200.0
            translateZ = -200.0
        }
        val pointLight2 = PointLight(Color.WHITE).apply {
            translateX = -200.0
            translateY = 200.0
            translateZ = 200.0
        }
        world.children.addAll(ambientLight, pointLight1, pointLight2)
    }

    private fun addAxes(world: Group, size: Double) {
        val thickness = 2.0

        val xAxis = Box(size, thickness, thickness).apply {
            material = PhongMaterial(Color.RED)
        }
        val yAxis = Box(thickness, size, thickness).apply {
            material = PhongMaterial(Color.GREEN)
        }
        val zAxis = Box(thickness, thickness, size).apply {
            material = PhongMaterial(Color.BLUE)
        }

        world.children.addAll(xAxis, yAxis, zAxis)
    }
}
