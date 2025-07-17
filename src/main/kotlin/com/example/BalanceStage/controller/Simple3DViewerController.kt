package com.example.BalanceStage.controller

import javafx.application.Platform
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.*
import javafx.scene.control.Label
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.paint.Color
import javafx.scene.shape.MeshView
import javafx.scene.transform.Rotate
import javafx.scene.transform.Translate
import javafx.stage.Stage
import org.fxyz3d.importers.Importer3D
import org.springframework.stereotype.Component
import java.io.File
import java.net.URL
import kotlin.math.max

@Component
class Simple3DViewerController {

    @FXML private lateinit var subScene: SubScene
    @FXML private lateinit var root3D: Group
    @FXML private lateinit var camera: PerspectiveCamera
    @FXML private lateinit var status: Label

    private val camGroup = Group()
    private var model: Node? = null
    private var anchorX = 0.0
    private var anchorY = 0.0

    @FXML
    fun initialize() {
        camera.fieldOfView = 60.0
        camera.nearClip = 0.1
        camera.farClip = 10000.0

        camGroup.children += camera
        root3D.children += camGroup

        // 깊이 버퍼 활성화
        subScene.depthTest = DepthTest.ENABLE

        // 기본 조명 추가
        val ambientLight = AmbientLight(Color.rgb(230, 230, 230))

        val pointLight1 = PointLight(Color.WHITE).apply {
            translateX = -1000.0
            translateY = -1000.0
            translateZ = -1000.0
        }

        val pointLight2 = PointLight(Color.WHITE).apply {
            translateX = 1000.0
            translateY = 800.0
            translateZ = 1200.0
        }

        root3D.children += listOf(ambientLight, pointLight1, pointLight2)



        // 마우스 이벤트 핸들링
        subScene.onMousePressed = EventHandler(::rememberAnchor)
        subScene.onMouseDragged = EventHandler(::rotateCamera)
        subScene.onScroll = EventHandler(::zoomCamera)

        resetView()
        loadModel()
    }

    @FXML
    fun loadModel() {
        try {
            setStatus("OBJ 모델 로딩 중...")

            val objUrl: URL? = javaClass.getResource("/3d/Gear-1.obj")
            if (objUrl == null) {
                setStatus("[에러] Gear.obj 파일을 찾을 수 없습니다")
                return
            }

            val modelDir = File(objUrl.toURI()).parentFile
            println("모델 디렉토리: $modelDir")

            // MTL 파일 존재 여부 확인
            val mtlFile = File(modelDir, "Gear.mtl")
            if (!mtlFile.exists()) {
                setStatus("Gear.mtl 파일이 없어 색상이 적용되지 않을 수 있습니다")
            }

            // 모델 로딩
            val model3D = Importer3D.load(objUrl)
            println("모델 클래스: ${model3D.root.javaClass.name}")
            println("meshViews 개수: ${model3D.meshViews.size}")

            // 기존 모델 제거 후 교체
            model?.let(root3D.children::remove)
            model = model3D.root
            root3D.children += model!!

            // 카메라 위치 재설정
            Platform.runLater { fitCameraTo(model!!) }
            setStatus("모델 로드 완료 (${model3D.meshViews.size} meshes)")

        } catch (e: Exception) {
            e.printStackTrace()
            setStatus("로딩 오류: ${e.message}")
        }
    }

    @FXML
    fun resetView() {
        camGroup.transforms.clear()
        camera.translateX = 0.0
        camera.translateY = 0.0
        camera.translateZ = -400.0
        setStatus("뷰 리셋 완료")
    }

    @FXML
    fun closeViewer() {
        (subScene.scene?.window as? Stage)?.close()
    }

    private fun fitCameraTo(node: Node) {
        val b = node.boundsInParent
        val maxSize = max(max(b.width, b.height), b.depth).takeIf { it > 0 } ?: 100.0
        val radius = maxSize * 0.6

        model?.transforms?.setAll(
            Translate(-b.centerX + 135.0, -b.centerY + 135.0, -b.centerZ),
            Rotate(180.0, Rotate.Z_AXIS),
            Rotate(180.0, Rotate.Y_AXIS)
        )

        camGroup.transforms.clear()
        camGroup.transforms += Translate(0.0, 0.0, 0.0)

        model?.scaleX = 3.0
        model?.scaleY = 3.0
        model?.scaleZ = 3.0

        camera.nearClip = 0.1
        camera.farClip = radius * 20
        camera.translateZ = -radius * 4.0
    }

    private fun rememberAnchor(e: MouseEvent) {
        anchorX = e.sceneX
        anchorY = e.sceneY
    }

    private fun rotateCamera(e: MouseEvent) {
        val dx = e.sceneX - anchorX
        val dy = e.sceneY - anchorY
        anchorX = e.sceneX
        anchorY = e.sceneY
        camGroup.transforms += Rotate(-dy * 0.3, Rotate.X_AXIS)
        camGroup.transforms += Rotate(dx * 0.3, Rotate.Y_AXIS)
    }

    private fun zoomCamera(e: ScrollEvent) {
        camera.translateZ += e.deltaY * 0.5
    }

    private fun setStatus(msg: String) {
        Platform.runLater { status.text = msg }
        println("상태: $msg")
    }
}