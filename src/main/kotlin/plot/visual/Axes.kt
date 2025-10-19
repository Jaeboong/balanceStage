package plot.visual

import javafx.geometry.Pos
import javafx.scene.*
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.*
import javafx.scene.transform.Rotate
import javafx.scene.transform.Translate
import kotlin.math.*

/** 3D 축, 눈금, 그리드, 화살촉을 한 번에 구성하는 헬퍼 */
object Axes {

    /** 메인 엔트리
     * @param length 축 길이(±length/2로 배치)
     * @param tickStep 눈금 간격
     * @param axisRadius 축 두께(원기둥 반지름 느낌)
     * @param showGrid XY, YZ, ZX 평면 그리드 표시 여부
     * @param gridStep 그리드 간격
     */
    fun buildAxes(
        length: Double,
        tickStep: Double = length / 10.0,
        axisRadius: Double = max(0.5, length * 0.005),
        showGrid: Boolean = true,
        gridStep: Double = length / 10.0
    ): Group {
        val g = Group()

        // X(RED), Y(BLUE, Up), Z(GREEN)
        g.children += buildAxisWithTicks(
            axis = Axis.X, length = length, tickStep = tickStep,
            axisRadius = axisRadius, color = Color.RED
        )
        g.children += buildAxisWithTicks(
            axis = Axis.Y, length = length, tickStep = tickStep,
            axisRadius = axisRadius, color = Color.BLUE
        )
        g.children += buildAxisWithTicks(
            axis = Axis.Z, length = length, tickStep = tickStep,
            axisRadius = axisRadius, color = Color.GREEN
        )

        if (showGrid) {
            g.children += buildGridPlane(GridPlane.XY, length, gridStep, Color.gray(0.85))
            g.children += buildGridPlane(GridPlane.YZ, length, gridStep, Color.gray(0.9))
            g.children += buildGridPlane(GridPlane.ZX, length, gridStep, Color.gray(0.9))
        }
        return g
    }

    // ---------------- internals ----------------

    private enum class Axis { X, Y, Z }
    private enum class GridPlane { XY, YZ, ZX }

    private fun buildAxisWithTicks(
        axis: Axis,
        length: Double,
        tickStep: Double,
        axisRadius: Double,
        color: Color
    ): Group {
        val mat = PhongMaterial(color)
        val g = Group()

        // 본체(원기둥)
        val cyl = Cylinder(axisRadius, length).apply {
            material = mat
            drawMode = DrawMode.FILL
        }
        // 축 방향으로 회전
        when (axis) {
            Axis.X -> cyl.transforms.addAll(Rotate(90.0, Rotate.Z_AXIS))
            Axis.Y -> {} // 기본은 Y축 위로
            Axis.Z -> cyl.transforms.addAll(Rotate(90.0, Rotate.X_AXIS))
        }
        g.children += cyl

        // 화살촉(간단한 콘 메쉬)
        val head = makeCone(
            radius = axisRadius * 2.2,
            height = length * 0.06,
            color = color
        )
        // 끝점 위치(+ 방향)
        val tipTranslate = when (axis) {
            Axis.X -> Translate(length / 2.0, 0.0, 0.0).also {
                head.transforms.addAll(Rotate(90.0, Rotate.Z_AXIS))
            }
            Axis.Y -> Translate(0.0, length / 2.0, 0.0)
            Axis.Z -> Translate(0.0, 0.0, length / 2.0).also {
                head.transforms.addAll(Rotate(90.0, Rotate.X_AXIS))
            }
        }
        head.transforms.add(tipTranslate)
        g.children += head

        // 눈금 (작은 박스)
        val tickHalf = axisRadius * 1.6
        val tickDepth = axisRadius * 0.75
        val minPos = -length / 2.0
        val maxPos = +length / 2.0
        var t = 0.0
        while (true) {
            val pos = (t * tickStep)
            // 0 기준 +/− 모두 찍기
            for (s in listOf(-1.0, +1.0)) {
                val p = s * pos
                if (p in (minPos + 1e-6)..(maxPos - 1e-6)) {
                    val tick = when (axis) {
                        Axis.X -> Box(0.6, tickHalf * 2, tickDepth).apply {
                            material = PhongMaterial(Color.gray(0.2))
                            translateX = p
                        }
                        Axis.Y -> Box(tickHalf * 2, 0.6, tickDepth).apply {
                            material = PhongMaterial(Color.gray(0.2))
                            translateY = p
                        }
                        Axis.Z -> Box(tickDepth, tickHalf * 2, 0.6).apply {
                            material = PhongMaterial(Color.gray(0.2))
                            translateZ = p
                        }
                    }
                    g.children += tick
                }
            }
            t += 1.0
            if (pos > maxPos) break
        }

        return g
    }

    private fun buildGridPlane(plane: GridPlane, length: Double, step: Double, color: Color): Group {
        val mat = PhongMaterial(color)
        val g = Group()
        val half = length / 2.0

        // 선은 아주 얇은 Box로 그린다
        fun lineX(zOrY: Double) = Box(length, 0.2, 0.2).apply {
            material = mat
            translateX = 0.0
            when (plane) {
                GridPlane.XY -> { translateY = zOrY; translateZ = 0.0 }
                GridPlane.ZX -> { translateZ = zOrY; translateY = 0.0 }
                GridPlane.YZ -> {} // not used here
            }
        }
        fun lineZ(yOrX: Double) = Box(0.2, 0.2, length).apply {
            material = mat
            translateZ = 0.0
            when (plane) {
                GridPlane.XY -> { translateY = yOrX; translateX = 0.0 }
                GridPlane.ZX -> { translateX = yOrX; translateY = 0.0 }
                GridPlane.YZ -> {} // not used here
            }
        }
        fun lineY(xOrZ: Double) = Box(0.2, length, 0.2).apply {
            material = mat
            translateY = 0.0
            when (plane) {
                GridPlane.YZ -> { translateZ = xOrZ; translateX = 0.0 }
                else -> {}
            }
        }

        var v = -half
        while (v <= half + 1e-6) {
            when (plane) {
                GridPlane.XY -> {
                    g.children += lineX(v) // 평행 X
                    g.children += lineZ(v) // 평행 Z(=Y축 표시 방향)
                }
                GridPlane.ZX -> {
                    // ZX 평면: X방향 라인 + Z방향 라인
                    g.children += Box(length, 0.2, 0.2).apply {
                        material = mat; translateZ = v
                    }
                    g.children += Box(0.2, 0.2, length).apply {
                        material = mat; translateX = v
                    }
                }
                GridPlane.YZ -> {
                    // YZ 평면: Y방향 라인 + Z방향 라인
                    g.children += lineY(v)
                    g.children += Box(0.2, 0.2, length).apply {
                        material = mat; translateY = v
                    }
                }
            }
            v += step
        }
        return g
    }

    /** 간단한 콘(삼각형 메쉬) 생성 */
    private fun makeCone(radius: Double, height: Double, color: Color, sections: Int = 24): MeshView {
        val mesh = TriangleMesh()
        // top + base ring
        mesh.points.addAll(0f, (-height/2).toFloat(), 0f) // top
        for (i in 0 until sections) {
            val th = 2.0 * Math.PI * i / sections
            val x = (radius * cos(th)).toFloat()
            val z = (radius * sin(th)).toFloat()
            mesh.points.addAll(x, (height/2).toFloat(), z)
        }
        // dummy UVs
        mesh.texCoords.addAll(0f,0f)

        // side faces
        for (i in 0 until sections) {
            val a = 0
            val b = 1 + i
            val c = 1 + ((i + 1) % sections)
            mesh.faces.addAll(a,0, b,0, c,0)
        }
        // base (fan)
        val baseCenterIndex = mesh.points.size() / 3
        mesh.points.addAll(0f, (height/2).toFloat(), 0f)
        for (i in 0 until sections) {
            val b = 1 + i
            val c = 1 + ((i + 1) % sections)
            mesh.faces.addAll(baseCenterIndex,0, c,0, b,0)
        }

        return MeshView(mesh).apply {
            material = PhongMaterial(color)
            drawMode = DrawMode.FILL
            cullFace = CullFace.BACK
        }
    }

    /** 오른쪽 위에 띄우는 간단한 컬러 범례(HUD) */
    fun buildAxisLegendHud(sceneRoot: Group): Node {
        val box = HBox(10.0).apply {
            alignment = Pos.TOP_RIGHT
            style = "-fx-padding: 10; -fx-background-color: transparent;"
            children.addAll(
                legendTag("X", Color.RED),
                legendTag("Y", Color.BLUE),
                legendTag("Z", Color.GREEN),
            )
            isMouseTransparent = true
        }
        return box
    }
    private fun legendTag(text: String, color: Color): Node {
        val r = javafx.scene.shape.Rectangle(14.0, 14.0).apply {
            arcWidth = 4.0; arcHeight = 4.0; fill = color
        }
        val t = javafx.scene.text.Text(text).apply {
            fill = Color.WHITE; style = "-fx-font-size: 14px; -fx-font-weight: bold;"
        }
        val h = HBox(6.0, r, t).apply {
            style = "-fx-background-color: rgba(20,20,25,0.55); -fx-padding: 6 8; -fx-background-radius: 8;"
            alignment = Pos.CENTER_LEFT
        }
        return h
    }
}
