package com.example.BalanceStage.service.impl

import com.example.BalanceStage.service.ColorMappingService
import com.example.BalanceStage.service.ContourVisualizationService
import com.example.BalanceStage.service.MeshFactory
import javafx.scene.*
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.Box
import javafx.scene.shape.MeshView
import javafx.scene.shape.Sphere
import javafx.scene.transform.Rotate
import org.springframework.stereotype.Service
import plot.geom.DistanceCalculator
import plot.geom.PlaneEstimator
import kotlin.math.abs
import kotlin.math.max

/**
 * PCA 기반 3D 등고선 시각화 서비스 구현
 * DIP: PlaneEstimator, DistanceCalculator, MeshFactory, ColorMappingService 인터페이스에 의존
 */
@Service
class PcaContourVisualizationService(
    private val planeEstimator: PlaneEstimator,
    private val distanceCalculator: DistanceCalculator,
    private val meshFactory: MeshFactory,
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

        // Step 1: Convert to arrays
        val xArray = points.map { it.first }.toDoubleArray()
        val yArray = points.map { it.second }.toDoubleArray()
        val zArray = points.map { it.third }.toDoubleArray()

        // Step 2: Estimate plane using PCA
        val plane = planeEstimator.estimate(xArray, yArray, zArray)
        val (cx, cy, cz) = plane.centroid
        val (nx, ny, nz) = plane.normal

        println("✓ PCA 평면 추정 완료:")
        println("  중심점: (${String.format("%.3f", cx)}, ${String.format("%.3f", cy)}, ${String.format("%.3f", cz)})")
        println("  법선벡터: (${String.format("%.3f", nx)}, ${String.format("%.3f", ny)}, ${String.format("%.3f", nz)})")

        // Step 3: Calculate deviations using DistanceCalculator
        val pointsArray = points.map { doubleArrayOf(it.first, it.second, it.third) }.toTypedArray()
        val deviations = distanceCalculator.signedDistances(pointsArray, plane).toList()

        val minDev = deviations.minOrNull() ?: -1.0
        val maxDev = deviations.maxOrNull() ?: 1.0
        val devRange = max(abs(minDev), abs(maxDev))

        println("  편차 범위: [${String.format("%.4f", minDev)}, ${String.format("%.4f", maxDev)}]")
        deviations.forEachIndexed { i, dev ->
            println("    P${i+1}: ${String.format("%+.4f", dev)}")
        }

        // Step 4: Auto-scaling
        val xRange = (xArray.maxOrNull()!! - xArray.minOrNull()!!)
        val yRange = (yArray.maxOrNull()!! - yArray.minOrNull()!!)
        val zRange = (zArray.maxOrNull()!! - zArray.minOrNull()!!)

        // Z 스케일을 크게 확대 (높이 차이를 시각적으로 명확하게)
        val zScale = 100.0
        val maxRange = max(max(xRange, yRange), zRange * zScale)
        val scale = if (maxRange > 0) 200.0 / maxRange else 1.0

        println("  좌표 범위: X=$xRange, Y=$yRange, Z=$zRange (실제 높이)")
        println("  Z 스케일: $zScale, 전체 스케일: $scale")

        // Step 5: Create 3D world
        val world = Group()

        // Add lighting
        addLighting(world)

        // Add plane mesh (평면을 XY 평면에 배치)
        val planeSize = max(xRange, yRange) * scale * 1.2
        val planeMesh = meshFactory.createPlaneMesh(planeSize, planeSize)
        val planeMeshView = MeshView(planeMesh).apply {
            material = PhongMaterial(Color.color(0.5, 0.5, 0.5, 0.3))
            cullFace = javafx.scene.shape.CullFace.NONE
            // 평면은 Z=0에 배치 (평면의 중심 높이)
            translateZ = 0.0
        }
        world.children.add(planeMeshView)

        // Add coordinate axes
        addAxes(world, planeSize)

        // Step 6: Add points with rainbow colors based on Z height
        val minZ = zArray.minOrNull()!!
        val maxZ = zArray.maxOrNull()!!
        val zRangeForColor = if (maxZ - minZ < 0.001) 1.0 else maxZ - minZ

        val transformedPoints = mutableListOf<Triple<Double, Double, Double>>()

        points.forEachIndexed { index, (x, y, z) ->
            // Z값 기준으로 색상 결정 (낮음 = 보라, 높음 = 빨강)
            val zNormalized = ((z - minZ) / zRangeForColor - 0.5) * 2.0  // [-1, 1] 범위
            val color = colorMapper.mapDeviationToColor(zNormalized, 1.0)

            // Center and scale - Z는 평면으로부터의 실제 높이 차이
            val scaledX = (x - cx) * scale
            val scaledY = (y - cy) * scale
            val scaledZ = (z - plane.centroid[2]) * zScale * scale  // 평면 중심 Z 기준

            transformedPoints.add(Triple(scaledX, scaledY, scaledZ))

            // Add sphere at position
            val sphere = Sphere(8.0).apply {
                material = PhongMaterial(color)
                translateX = scaledX
                translateY = scaledY
                translateZ = scaledZ
            }
            world.children.add(sphere)

            println("  P${index+1} 위치: (${String.format("%.1f", scaledX)}, ${String.format("%.1f", scaledY)}, ${String.format("%.1f", scaledZ)}) - Z원본=${String.format("%.3f", z)}")
        }

        // Step 7: Add contour lines connecting points
        addContourLines(world, transformedPoints, zArray.toList(), minZ, maxZ)

        // Step 8: Setup camera for top-down view
        val camera = PerspectiveCamera(true).apply {
            nearClip = 0.1
            farClip = 10000.0
            // 카메라를 위에서 내려다보도록 배치
            translateX = 0.0
            translateY = 0.0
            translateZ = -planeSize * 2.5
        }

        val camGroup = Group().apply {
            children.add(camera)
            transforms.addAll(
                Rotate(-60.0, Rotate.X_AXIS),  // 위에서 60도 각도로 내려다봄
                Rotate(0.0, Rotate.Y_AXIS),
                Rotate(-30.0, Rotate.Z_AXIS)   // 30도 회전하여 대각선 시점
            )
        }

        world.children.add(camGroup)

        // Step 7: Create SubScene
        return SubScene(world, width, height, true, SceneAntialiasing.BALANCED).apply {
            fill = Color.color(0.05, 0.05, 0.05)
            this.camera = camera
        }
    }

    private fun addLighting(world: Group) {
        val ambientLight = AmbientLight(Color.color(0.5, 0.5, 0.5))
        val pointLight1 = PointLight(Color.WHITE).apply {
            translateX = 200.0
            translateY = -200.0
            translateZ = -200.0
        }
        val pointLight2 = PointLight(Color.color(0.3, 0.3, 0.3)).apply {
            translateX = -200.0
            translateY = 200.0
            translateZ = 200.0
        }
        world.children.addAll(ambientLight, pointLight1, pointLight2)
    }

    private fun addAxes(world: Group, planeSize: Double) {
        val axisLength = planeSize * 0.6
        val axisThickness = 3.0

        val xAxis = Box(axisLength, axisThickness, axisThickness).apply {
            material = PhongMaterial(Color.RED)
        }
        val yAxis = Box(axisThickness, axisLength, axisThickness).apply {
            material = PhongMaterial(Color.GREEN)
        }
        val zAxis = Box(axisThickness, axisThickness, axisLength).apply {
            material = PhongMaterial(Color.BLUE)
        }

        world.children.addAll(xAxis, yAxis, zAxis)
    }


    /**
     * 등고선 추가 (점들을 순서대로 연결하여 다각형 형태로 표시)
     */
    private fun addContourLines(
        world: Group,
        points: List<Triple<Double, Double, Double>>,
        zValues: List<Double>,
        minZ: Double,
        maxZ: Double
    ) {
        if (points.size < 2) return

        val zRange = if (maxZ - minZ < 0.001) 1.0 else maxZ - minZ

        // Connect points in sequence with colored cylinders
        for (i in 0 until points.size) {
            val nextIdx = (i + 1) % points.size

            val p1 = points[i]
            val p2 = points[nextIdx]

            // Use average Z value for line color
            val avgZ = (zValues[i] + zValues[nextIdx]) / 2.0
            val zNormalized = ((avgZ - minZ) / zRange - 0.5) * 2.0
            val color = colorMapper.mapDeviationToColor(zNormalized, 1.0)

            val line = createCylinderLine(p1, p2, 2.5, color)
            world.children.add(line)
        }
    }

    /**
     * 두 점을 연결하는 원기둥 생성 (3D 선)
     */
    private fun createCylinderLine(
        start: Triple<Double, Double, Double>,
        end: Triple<Double, Double, Double>,
        radius: Double,
        color: Color
    ): Group {
        val dx = end.first - start.first
        val dy = end.second - start.second
        val dz = end.third - start.third

        val length = kotlin.math.sqrt(dx*dx + dy*dy + dz*dz)

        if (length < 0.001) {
            return Group() // Too short, skip
        }

        val cylinder = javafx.scene.shape.Cylinder(radius, length).apply {
            material = PhongMaterial(color)
        }

        val midX = (start.first + end.first) / 2.0
        val midY = (start.second + end.second) / 2.0
        val midZ = (start.third + end.third) / 2.0

        // Rotation to align with line direction
        val yAxis = Rotate.Y_AXIS
        val lineDir = javafx.geometry.Point3D(dx, dy, dz).normalize()
        val yDir = javafx.geometry.Point3D(0.0, 1.0, 0.0)

        val rotationAxis = yDir.crossProduct(lineDir)
        val angle = kotlin.math.acos(yDir.dotProduct(lineDir).coerceIn(-1.0, 1.0))

        val group = Group(cylinder)
        group.translateX = midX
        group.translateY = midY
        group.translateZ = midZ

        if (rotationAxis.magnitude() > 0.001) {
            group.transforms.add(
                Rotate(
                    Math.toDegrees(angle),
                    rotationAxis.x,
                    rotationAxis.y,
                    rotationAxis.z
                )
            )
        }

        return group
    }

}
