package com.example.BalanceStage.controller

import com.example.BalanceStage.util.BalanceSimulator
import com.example.BalanceStage.util.PCAPlaneCalculator
import com.example.BalanceStage.util.ContourRenderer
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import org.springframework.stereotype.Component
import java.net.URL
import java.util.*

/**
 * 밸런스 시뮬레이션 테스트 전용 컨트롤러
 * 프로덕션 코드(HelloController)와 분리된 독립적인 시연 환경
 */
@Component
class BalanceSimulationController : Initializable {

    // UI Components
    @FXML private var contourBox: Pane? = null
    @FXML private var statusLabel: Label? = null
    @FXML private var startStopBtn: Button? = null
    @FXML private var pointListBox: VBox? = null
    @FXML private var logArea: TextArea? = null

    // Data
    private val pointList = mutableListOf<PointData>()
    private var contourCanvas: Canvas? = null
    private val contourRenderer = ContourRenderer()

    // Simulation
    private var balanceSimulator: BalanceSimulator? = null
    private var isSimulationRunning = false

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        // 테스트용 초기 데이터 로드
        loadTestData()

        // Ctrl+S 단축키 설정
        setupShortcuts()

        // 버튼 핸들러
        startStopBtn?.setOnAction { toggleSimulation() }
    }

    /**
     * 단축키 설정
     */
    private fun setupShortcuts() {
        Platform.runLater {
            contourBox?.scene?.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
                if (event.isControlDown && event.code == KeyCode.S) {
                    toggleSimulation()
                    event.consume()
                }
            }
        }
    }

    /**
     * 테스트용 데이터 로드 (TK#1)
     */
    private fun loadTestData() {
        pointList.clear()
        pointList.addAll(listOf(
            PointData("P1", 0.0, 120.0, 1.95, 0.0),
            PointData("P2", 0.0, 0.0, 1.654, 0.0),
            PointData("P3", 121.0, 0.0, 1.798, 0.0)
        ))

        // 초기 편차 계산 및 렌더링
        recalculateDeviations()
        renderContour()

        log("테스트 데이터 로드 완료 (TK#1, 3개 점)")
    }

    /**
     * 시뮬레이션 시작/중지 토글
     */
    private fun toggleSimulation() {
        if (isSimulationRunning) {
            stopSimulation()
        } else {
            startSimulation()
        }
    }

    /**
     * 시뮬레이션 시작
     */
    private fun startSimulation() {
        if (pointList.size < 3) {
            log("❌ 시뮬레이션: 최소 3개의 점이 필요합니다")
            return
        }

        balanceSimulator = BalanceSimulator(
            points = pointList,
            updateCallback = {
                // Z값 변경 후 즉시 편차 재계산 및 렌더링
                Platform.runLater {
                    recalculateDeviations()
                    renderContour()
                }
            },
            statusCallback = { msg ->
                Platform.runLater {
                    log(msg)
                    statusLabel?.text = msg
                }
            }
        )

        balanceSimulator?.start()
        isSimulationRunning = true
        startStopBtn?.text = "중지 (Ctrl+S)"
        log("⚙ 밸런스 시뮬레이션 시작")
    }

    /**
     * 시뮬레이션 중지
     */
    private fun stopSimulation() {
        balanceSimulator?.stop()
        balanceSimulator = null
        isSimulationRunning = false
        startStopBtn?.text = "시작 (Ctrl+S)"
        log("⏸ 시뮬레이션 중지됨")
        statusLabel?.text = "대기 중..."
    }

    /**
     * PCA 평면 기준 편차 재계산 (외부 호출용)
     * Z값이 변경된 후 호출되어 편차를 실시간으로 업데이트
     */
    fun recalculateDeviationsFor(points: MutableList<PointData>) {
        if (points.size < 3) return

        // 1. Point3D로 변환
        val pts = points.map { PCAPlaneCalculator.Point3D(it.x, it.y, it.z) }

        // 2. PCA 평면 계산
        val plane = PCAPlaneCalculator.calculateBestFitPlane(pts) ?: return

        // 3. 편차 계산
        val deviations = PCAPlaneCalculator.calculateDeviations(pts, plane)

        // 4. PointData + UI 동기화
        points.forEachIndexed { index, p ->
            if (index < deviations.size) {
                val (_, _, dev) = deviations[index]
                p.deviation = dev

                // ✅ Z값도 UI에 반영 (시뮬레이션 중 변경된 값)
                p.zField?.text = String.format("%.3f", p.z)
                p.deviField?.text = String.format("%.4f", dev)
            }
        }
    }

    /**
     * PCA 평면 기준 편차 재계산 (내부용)
     * Z값이 변경된 후 호출되어 편차를 실시간으로 업데이트
     */
    private fun recalculateDeviations() {
        if (pointList.size < 3) return

        // 1. Point3D로 변환
        val pts = pointList.map { PCAPlaneCalculator.Point3D(it.x, it.y, it.z) }

        // 2. PCA 평면 계산
        val plane = PCAPlaneCalculator.calculateBestFitPlane(pts) ?: return

        // 3. 편차 계산
        val deviations = PCAPlaneCalculator.calculateDeviations(pts, plane)

        // 4. PointData 업데이트
        pointList.forEachIndexed { index, p ->
            if (index < deviations.size) {
                val (_, _, dev) = deviations[index]
                p.deviation = dev
                p.deviField?.text = String.format("%.4f", dev)
            }
        }
    }

    /**
     * 등고선 렌더링
     */
    private fun renderContour() {
        try {
            if (pointList.size < 3) return

            // 1. Point3D로 변환
            val pts = pointList.map { PCAPlaneCalculator.Point3D(it.x, it.y, it.z) }

            // 2. PCA 평면 계산
            val plane = PCAPlaneCalculator.calculateBestFitPlane(pts)

            val deviations = if (plane != null) {
                PCAPlaneCalculator.calculateDeviations(pts, plane)
            } else {
                val avgZ = pointList.map { it.z }.average()
                pointList.map { p -> Triple(p.x, p.y, p.z - avgZ) }
            }

            // 3. Canvas 크기
            val boxWidth = (contourBox?.width?.takeIf { it > 0.0 }) ?: 420.0
            val boxHeight = (contourBox?.height?.takeIf { it > 0.0 }) ?: 420.0
            val canvasWidth = 420.0

            // 4. 렌더링 설정
            val config = ContourRenderer.ContourConfig(
                gridSize = 200,
                numContours = 20,
                sigma = 0.30,
                colorMap = ContourRenderer.ColorMap.SPECTRAL
            )

            // 5. Canvas 재사용
            Platform.runLater {
                if (contourCanvas == null ||
                    contourCanvas?.width != canvasWidth ||
                    contourCanvas?.height != boxHeight) {
                    contourBox?.children?.removeIf { it is Canvas }
                    contourCanvas = Canvas(canvasWidth, boxHeight)
                    contourBox?.children?.add(contourCanvas!!)
                }

                val gc = contourCanvas!!.graphicsContext2D
                gc.clearRect(0.0, 0.0, canvasWidth, boxHeight)

                val renderer = ContourRenderer(canvasWidth, boxHeight)
                renderer.renderToGraphicsContext(gc, deviations, config)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            log("등고선 렌더 실패: ${e.message}")
        }
    }

    /**
     * 로그 출력
     */
    private fun log(message: String) {
        val timestamp = java.time.LocalTime.now().toString().substring(0, 8)
        logArea?.appendText("[$timestamp] $message\n")
        println("[$timestamp] $message")
    }
}
