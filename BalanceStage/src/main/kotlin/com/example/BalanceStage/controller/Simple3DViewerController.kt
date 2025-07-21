package com.example.BalanceStage.controller

import com.example.BalanceStage.controller.Simple3DViewerController
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.*
import javafx.scene.control.Label
import javafx.scene.input.MouseButton
import javafx.scene.input.ScrollEvent
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.Box
import javafx.scene.shape.CullFace
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.transform.Rotate
import javafx.scene.transform.Translate
import javafx.stage.Stage
import javafx.util.Duration
import org.fxyz3d.importers.Importer3D
import org.springframework.stereotype.Component
import java.io.File
import kotlin.math.max

// ResultData 를 import 해 줍니다
import com.example.BalanceStage.controller.ResultData

@Component
class Simple3DViewerController {

    companion object {
        /** HelloController 에서 세팅해 주는 최신 결과 데이터 */
        var lastResultData: ResultData? = null

        private const val AXIS_THICKNESS_FACTOR = 0.05
        private const val LABEL_FONT_SIZE       = 24.0

        private const val MODEL_OFFSET_X = 50.0
        private const val MODEL_OFFSET_Y = 60.0

        private const val XZ_SCALE_FACTOR = 4.0
        private const val Y_SCALE_FACTOR  = 2.0
    }

    // ——— 여기에 추가된 휠/각도 레이블 바인딩 ———
    @FXML private lateinit var wheelDelta1Label: Label
    @FXML private lateinit var wheelDelta2Label: Label
    @FXML private lateinit var bRpAngleLabel:     Label
    @FXML private lateinit var aRpAngleLabel:     Label
    // ————————————————————————————————

    @FXML private lateinit var subScene: SubScene
    @FXML private lateinit var root3D:    Group
    @FXML private lateinit var camera:   PerspectiveCamera
    @FXML private lateinit var status:   Label

    private val camGroup   = Group()
    private val axisGroup  = Group()
    private val planeGroup = Group()

    private val planeMaterial = PhongMaterial().apply {
        diffuseColor  = Color.GRAY
        specularColor = Color.GRAY
    }

    private var modelNode: Node? = null
    private var anchorX = 0.0
    private var anchorY = 0.0

    private var axisLengthX   = 100.0
    private var axisLengthY   = 100.0
    private var axisLengthZ   = 100.0
    private var axisThickness = 2.0
    private var showAxes      = true

    @FXML
    fun initialize() {
        // 카메라 설정
        camera.fieldOfView = 60.0
        camera.nearClip    = 0.1
        camera.farClip     = 10000.0

        // 그룹에 카메라 추가
        camGroup.children += camera
        root3D.children   += camGroup

        subScene.depthTest = DepthTest.ENABLE
        subScene.fill      = Color.WHITE

        // 조명
        root3D.children += AmbientLight(Color.rgb(230,230,230))
        root3D.children += PointLight(Color.WHITE).apply {
            translateX = -1000.0; translateY = -1000.0; translateZ = -1000.0
        }
        root3D.children += PointLight(Color.WHITE).apply {
            translateX = 1000.0; translateY =  800.0; translateZ = 1200.0
        }

        // 평면 & 축
        root3D.children += planeGroup
        root3D.children += axisGroup
        createPlane()
        createAxes()

        // 마우스 컨트롤
        subScene.setOnMousePressed { e ->
            anchorX = e.sceneX; anchorY = e.sceneY
        }
        subScene.setOnMouseDragged { e ->
            val dx = e.sceneX - anchorX
            val dy = e.sceneY - anchorY
            anchorX = e.sceneX; anchorY = e.sceneY
            when (e.button) {
                MouseButton.PRIMARY   -> {
                    camGroup.transforms += Rotate(-dy * 0.3, Rotate.X_AXIS)
                    camGroup.transforms += Rotate( dx * 0.3, Rotate.Y_AXIS)
                }
                MouseButton.MIDDLE    -> camGroup.transforms += Translate(-dx, -dy, 0.0)
                MouseButton.SECONDARY -> camera.translateZ += dy * 0.5
                else                  -> {}
            }
        }
        subScene.onScroll = EventHandler(::zoomCamera)

        // 모델 로드
        loadModel()

        // ——— 이 부분만 추가: 휠 변화량과 각도 값을 상단에 전달 ———
        lastResultData?.let { rd ->
            wheelDelta1Label.text = String.format("ΔWh1: %.3f°", rd.firstWheelMinus)
            wheelDelta2Label.text = String.format("ΔWh2: %.3f°", rd.secondWheelMinus)
            bRpAngleLabel.text     = String.format("B_P Angle: %.4f", rd.bpAngle)
            aRpAngleLabel.text     = String.format("A_P Angle: %.4f", rd.apAngle)
        }
        // ————————————————————————————————————————————————
    }

    /** 평면 그리기 */
    private fun createPlane() {
        planeGroup.children.clear()
        val sizeX = axisLengthX * 2
        val sizeZ = axisLengthZ * 2
        val plane = Box(sizeX, 0.01, sizeZ).apply {
            material  = planeMaterial
            cullFace  = CullFace.BACK
            translateY = -0.005
        }
        planeGroup.children += plane
    }

    /** 축 그리기 (X,Z축 화살 제거) */
    private fun createAxes() {
        axisGroup.children.clear()
        axisGroup.transforms.clear()

        val matX = PhongMaterial(Color.RED).apply   { specularColor = Color.RED }
        val matY = PhongMaterial(Color.GREEN).apply { specularColor = Color.GREEN }
        val matZ = PhongMaterial(Color.BLUE).apply  { specularColor = Color.BLUE }
        val matO = PhongMaterial(Color.BLACK).apply { specularColor = Color.BLACK }

        val xAxis = Box(axisLengthX * 2, axisThickness, axisThickness).apply { material = matX }
        val yAxis = Box(axisThickness, axisLengthY, axisThickness).apply {
            material   = matY
            translateY = -axisLengthY / 2
        }
        val zAxis = Box(axisThickness, axisThickness, axisLengthZ * 2).apply { material = matZ }

        axisGroup.children += listOf(
            xAxis, yAxis, zAxis,
            Text("X").apply    { fill = Color.RED  ; font = Font.font(LABEL_FONT_SIZE); translateX = axisLengthX + 10 },
            Text("-X").apply   { fill = Color.RED  ; font = Font.font(LABEL_FONT_SIZE); translateX = -axisLengthX - 30 },
            Text("Y").apply    { fill = Color.GREEN; font = Font.font(LABEL_FONT_SIZE); translateY = -axisLengthY/2 - 10 },
            Text("Z").apply    { fill = Color.BLUE ; font = Font.font(LABEL_FONT_SIZE); translateZ = axisLengthZ + 10; rotationAxis = Rotate.Y_AXIS; rotate = 90.0 },
            Text("-Z").apply   { fill = Color.BLUE ; font = Font.font(LABEL_FONT_SIZE); translateZ = -axisLengthZ - 30; rotationAxis = Rotate.Y_AXIS; rotate = 90.0 },
            Box(axisThickness*3, axisThickness*3, axisThickness*3).apply { material = matO }
        )

        axisGroup.depthTest         = DepthTest.DISABLE
        axisGroup.isMouseTransparent = true
        axisGroup.isVisible         = showAxes
    }

    /** 3D 모델 로드 */
    @FXML
    fun loadModel(event: ActionEvent? = null) {
        try {
            setStatus("OBJ 모델 로딩 중…")
            val url = javaClass.getResource("/3d/Gear-1.obj")
                ?: return setStatus("[에러] Gear-1.obj 파일을 찾을 수 없습니다")
            val parent = File(url.toURI()).parentFile
            if (!File(parent, "Gear.mtl").exists())
                setStatus("경고: MTL 파일이 없어 색상이 적용되지 않을 수 있습니다")

            val imp = Importer3D.load(url)
            modelNode?.let { root3D.children.remove(it) }
            modelNode = imp.root
            root3D.children += imp.root

            val b    = imp.root.boundsInParent
            val cx   = b.minX + b.width  / 2
            val cy   = b.minY + b.height / 2
            val cz   = b.minZ + b.depth  / 2
            val base = max(max(b.width, b.height), b.depth).takeIf { it>0 } ?: 100.0

            imp.root.transforms.setAll(
                Translate(-cx + MODEL_OFFSET_X, -cy + MODEL_OFFSET_Y, -cz),
                Rotate(180.0, Rotate.X_AXIS)
            )

            axisLengthX   = base * XZ_SCALE_FACTOR
            axisLengthY   = base * Y_SCALE_FACTOR
            axisLengthZ   = base * XZ_SCALE_FACTOR
            axisThickness = base * AXIS_THICKNESS_FACTOR

            createAxes()
            createPlane()

            Platform.runLater { fitCameraTo(imp.root) }
            setStatus("모델 로드 완료 (${imp.meshViews.size} meshes)")
        } catch (e: Exception) {
            e.printStackTrace()
            setStatus("로딩 오류: ${e.message}")
        }
    }

    @FXML
    fun resetView(event: ActionEvent? = null) {
        camGroup.transforms.clear()
        camera.translateX = 0.0
        camera.translateY = 0.0
        camera.translateZ = -400.0
        setStatus("뷰 리셋 완료")
    }

    @FXML
    fun toggleAxes(event: ActionEvent) {
        showAxes = !showAxes
        axisGroup.isVisible = showAxes
        setStatus(if (showAxes) "축 표시됨" else "축 숨김됨")
    }

    @FXML
    fun closeViewer(event: ActionEvent) {
        (subScene.scene.window as? Stage)?.close()
    }

    private fun fitCameraTo(node: Node) {
        val b       = node.boundsInParent
        val maxSize = max(max(b.width, b.height), b.depth).takeIf { it>0 } ?: 100.0
        val radius  = maxSize * 0.6
        node.scaleX = 3.0; node.scaleY = 3.0; node.scaleZ = 3.0
        camGroup.transforms.clear()
        camera.nearClip   = 0.1
        camera.farClip    = radius * 20
        camera.translateZ = -radius * 4.0
    }

    private fun zoomCamera(e: ScrollEvent) {
        camera.translateZ += e.deltaY * 0.5
    }

    private fun setStatus(msg: String) {
        Platform.runLater { status.text = msg }
        println("상태: $msg")
    }
}
