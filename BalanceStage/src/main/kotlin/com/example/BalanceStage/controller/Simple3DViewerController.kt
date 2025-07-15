package com.example.BalanceStage.controller

import javafx.application.Platform
import javafx.event.EventHandler          // ← 추가
import javafx.fxml.FXML
import javafx.scene.*
import javafx.scene.control.Label
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.paint.Color
import javafx.scene.transform.Rotate
import javafx.scene.transform.Translate
import javafx.stage.Stage
import org.fxyz3d.importers.Importer3D     // ← API 변경
import org.springframework.stereotype.Component
import java.net.URL
import kotlin.math.max

@Component
class Simple3DViewerController {

    /* ---------- FXML 바인딩 ---------- */
    @FXML private lateinit var subScene : SubScene
    @FXML private lateinit var root3D  : Group
    @FXML private lateinit var camera  : PerspectiveCamera
    @FXML private lateinit var status  : Label

    /* ---------- 내부 상태 ---------- */
    private val camGroup = Group()
    private var model    : Node? = null
    private var anchorX = 0.0
    private var anchorY = 0.0

    @FXML
    fun initialize() {
        println("Simple3DViewer 초기화 시작...")

        // 카메라 & 라이트 ----------------------------------------------------
        camera.fieldOfView = 60.0
        camera.nearClip = 0.1
        camera.farClip  = 10_000.0
        camGroup.children += camera
        root3D.children  += camGroup

        root3D.children += AmbientLight(Color.gray(0.3))
        root3D.children += PointLight(Color.gray(0.7)).apply {
            translateX = 500.0; translateY = -500.0; translateZ = -500.0
        }

        // 이벤트 핸들러 -------------------------------------------------------
        subScene.onMousePressed = EventHandler(::rememberAnchor)
        subScene.onMouseDragged = EventHandler(::rotateCamera)
        subScene.onScroll       = EventHandler(::zoomCamera)

        resetView()
        setStatus("3D 뷰어 준비 완료")
        println("Simple3DViewer 초기화 완료!")
    }

    /* ---------- 모델 로드 ---------- */
    @FXML
    fun loadModel() {
        try {
            setStatus("GLB 모델 로딩 중...")
            val url: URL = javaClass.getResource("/3d/3D.glb")
                ?: return setStatus("3d/3D.glb 파일을 찾을 수 없습니다")

            // 0.6 API → 한 줄이면 끝!
            val model3D   = Importer3D.load(url)
            val meshCount = model3D.meshViews.size

            // 이전 모델 제거 & 새 모델 추가
            model?.let(root3D.children::remove)
            model = model3D.root
            root3D.children += model!!

            // 바운딩 계산 뒤 카메라 맞춤
            Platform.runLater { fitCameraTo(model!!) }

            setStatus("모델 로드 완료 ($meshCount meshes)")
            println("메시 개수: $meshCount")

        } catch (e: Exception) {
            e.printStackTrace()
            setStatus("로딩 오류: ${e.message}")
        }
    }

    /* ---------- 뷰 조작 ---------- */
    @FXML fun resetView() {
        camGroup.transforms.clear()
        camera.translateX = 0.0
        camera.translateY = 0.0
        camera.translateZ = -400.0
        setStatus("뷰 리셋 완료")
    }
    @FXML fun closeViewer() {
        (subScene.scene?.window as? Stage)?.close()
    }

    private fun fitCameraTo(node: Node) {
        val b = node.boundsInParent
        val maxSize = max(max(b.width, b.height), b.depth)
        val radius  = maxSize * 0.6

        camGroup.transforms.setAll(Translate(-b.centerX, -b.centerY, -b.centerZ))
        camera.nearClip    = radius * 0.01
        camera.farClip     = radius * 20
        camera.translateZ  = -radius * 2.5
    }

    /* ---------- 마우스 이벤트 ---------- */
    private fun rememberAnchor(e: MouseEvent) { anchorX = e.sceneX; anchorY = e.sceneY }
    private fun rotateCamera(e: MouseEvent) {
        val dx = e.sceneX - anchorX
        val dy = e.sceneY - anchorY
        anchorX = e.sceneX; anchorY = e.sceneY
        camGroup.transforms += Rotate(-dy * .3, Rotate.X_AXIS)
        camGroup.transforms += Rotate( dx * .3, Rotate.Y_AXIS)
    }
    private fun zoomCamera(e: ScrollEvent) { camera.translateZ += e.deltaY * .5 }

    /* ---------- 상태 ---------- */
    private fun setStatus(msg: String) {
        Platform.runLater { status.text = msg }
        println("상태: $msg")
    }
}
