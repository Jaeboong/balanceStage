package com.example.BalanceStage.controller

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.geometry.Insets
import javafx.scene.text.Font
import javafx.stage.Stage
import org.springframework.stereotype.Component
import java.net.URL
import java.util.*

// Point 데이터 클래스
data class PointData(
    var pointName: String,
    var x: Double = 0.0,
    var y: Double = 0.0,
    var z: Double = 0.0,
    var deviation: Double = 0.0
)

// 결과 데이터 클래스
data class ResultData(
    val name: String,
    val points: List<PointData>,
    val bpDirection: Double,
    val bpAngle: Double,
    val apDirection: Double,
    val apAngle: Double,
    val centerShiftX: Double,
    val centerShiftY: Double,
    val centerShiftZ: Double,
    val firstWheel: Double,
    val secondWheel: Double,
    val firstWheelMinus: Double,
    val secondWheelMinus: Double,
    val angleTracking: Double,
    val productCode: String,
    val recipe: String
)

@Component
class HelloController : Initializable {

    // Point 관리를 위한 리스트
    private val pointList = mutableListOf<PointData>()
    private val pointUIList = mutableListOf<HBox>()
    private var pointCounter = 1
    
    // 미리 정의된 데이터셋들
    private val predefinedResults = listOf(
        ResultData(
            name = "TK#1",
            points = listOf(
                PointData("P1", 0.0, 120.0, 1.95, 0.0),
                PointData("P2", 0.0, 0.0, 1.654, 0.0),
                PointData("P3", 121.0, 0.0, 1.798, 0.0)
            ),
            bpDirection = 261.128,
            bpAngle = 0.9164,
            apDirection = 154.244,
            apAngle = 0.1569,
            centerShiftX = -0.388,
            centerShiftY = 0.050,
            centerShiftZ = 39.997,
            firstWheel = 29.216,
            secondWheel = 349.720,
            firstWheelMinus = -330.784,
            secondWheelMinus = -10.280,
            angleTracking = 1.0,
            productCode = "-",
            recipe = "Space Bit"
        ),
        ResultData(
            name = "TK#2",
            points = listOf(
                PointData("P1", 0.0, 120.0, 0.998, 0.0),
                PointData("P2", 0.0, 0.0, 0.856, 0.0),
                PointData("P3", 121.0, 0.0, 0.894, 0.0)
            ),
            bpDirection = 265.942,
            bpAngle = 0.9581,
            apDirection = 165.137,
            apAngle = 0.0701,
            centerShiftX = -0.413,
            centerShiftY = 0.045,
            centerShiftZ = 39.997,
            firstWheel = 14.628,
            secondWheel = 354.090,
            firstWheelMinus = -345.372,
            secondWheelMinus = -5.910,
            angleTracking = 1.0,
            productCode = "-",
            recipe = "Space Bit"
        ),
        ResultData(
            name = "TK#3",
            points = listOf(
                PointData("P1", 0.0, 120.0, 0.56, 0.0),
                PointData("P2", 0.0, 0.0, 0.75, 0.0),
                PointData("P3", 121.0, 0.0, 1.566, 0.0)
            ),
            bpDirection = -81.219,
            bpAngle = 0.5942,
            apDirection = 76.787,
            apAngle = 0.3969,
            centerShiftX = -0.263,
            centerShiftY = 0.027,
            centerShiftZ = 39.999,
            firstWheel = 45.792,
            secondWheel = 300.957,
            firstWheelMinus = -314.208,
            secondWheelMinus = -59.043,
            angleTracking = 1.0,
            productCode = "-",
            recipe = "Space Bit"
        ),
        ResultData(
            name = "TK#4",
            points = listOf(
                PointData("P1", 0.0, 120.0, 1.11, 0.0),
                PointData("P2", 0.0, 0.0, 1.553, 0.0),
                PointData("P3", 121.0, 0.0, 1.763, 0.0)
            ),
            bpDirection = -76.398,
            bpAngle = 0.8995,
            apDirection = 25.179,
            apAngle = 0.2337,
            centerShiftX = -0.386,
            centerShiftY = -0.060,
            centerShiftZ = 39.997,
            firstWheel = 9.612,
            secondWheel = 324.562,
            firstWheelMinus = -350.388,
            secondWheelMinus = -35.438,
            angleTracking = 1.0,
            productCode = "-",
            recipe = "Space Bit"
        ),
        ResultData(
            name = "TK#5",
            points = listOf(
                PointData("P1", 0.0, 120.0, -0.359, 0.0),
                PointData("P2", 0.0, 0.0, -1.017, 0.0),
                PointData("P3", 121.0, 0.0, 1.763, 0.0)
            ),
            bpDirection = 132.524,
            bpAngle = 0.4647,
            apDirection = 103.423,
            apAngle = 1.3531,
            centerShiftX = 0.196,
            centerShiftY = 0.079,
            centerShiftZ = 39.999,
            firstWheel = 202.072,
            secondWheel = 78.996,
            firstWheelMinus = -157.928,
            secondWheelMinus = -281.004,
            angleTracking = 1.0,
            productCode = "-",
            recipe = "Space Bit"
        )
    )

    // Main UI components
    @FXML private var pageMainPane: HBox? = null
    @FXML private var pageRecipePane: VBox? = null
    @FXML private var pageIOCommPane: VBox? = null
    @FXML private var pageOptionPane: VBox? = null
    @FXML private var dynamicContainer: VBox? = null
    
    // Tab components
    @FXML private var coordinateInput: Tab? = null
    @FXML private var compassInput: Tab? = null
    @FXML private var azimuthInput: Tab? = null
    
    // Input fields for coordinate input
    @FXML private var compassDirection: TextField? = null
    @FXML private var compassRadius: TextField? = null
    @FXML private var compassHeight: TextField? = null
    @FXML private var azimuthDirection: TextField? = null
    @FXML private var azimuthRadius: TextField? = null
    @FXML private var azimuthHeight: TextField? = null
    @FXML private var coordinateX: TextField? = null
    @FXML private var coordinateY: TextField? = null
    @FXML private var coordinateZ: TextField? = null
    
    // Table view for point data
    @FXML private var pointsTable: TableView<*>? = null
    @FXML private var pointX: TableColumn<*, *>? = null
    @FXML private var pointY: TableColumn<*, *>? = null
    @FXML private var pointZ: TableColumn<*, *>? = null
    @FXML private var pointComment: TableColumn<*, *>? = null
    
    // Control buttons
    @FXML private var addPointInputBtn: Button? = null
    @FXML private var deletePointInputBtn: Button? = null
    @FXML private var savePointsInfoBtn: Button? = null
    @FXML private var inputInfoCombo: ComboBox<String>? = null
    
    // Result labels
    @FXML private var bRpDirLabel: Label? = null
    @FXML private var bRpAngLabel: Label? = null
    @FXML private var aRpDirLabel: Label? = null
    @FXML private var aRpAngLabel: Label? = null
    @FXML private var shiftXLabel: Label? = null
    @FXML private var shiftYLabel: Label? = null
    @FXML private var shiftZLabel: Label? = null
    @FXML private var firstWheelText: Label? = null
    @FXML private var secondWheelText: Label? = null
    @FXML private var firstMinusWheelText: Label? = null
    @FXML private var secondMinusWheelText: Label? = null
    @FXML private var inAngleMax: Label? = null
    @FXML private var productCodeLabel: Label? = null
    @FXML private var nameOfRecipe: Label? = null
    
    // Wheel position inputs
    @FXML private var oldFirstWheelPos: TextField? = null
    @FXML private var oldSecondWheelPos: TextField? = null
    
    // Navigation buttons
    @FXML private var recipePageBtn: Button? = null
    @FXML private var ioCommPageBtn: Button? = null
    @FXML private var mainPageBtn: Button? = null
    @FXML private var optionPageBtn: Button? = null
    
    // Communication components
    @FXML private var comPortMotorCombo: ComboBox<String>? = null
    @FXML private var mainComPortPane: Pane? = null
    @FXML private var logTextArea: TextArea? = null
    @FXML private var sendMessageBtn: Button? = null
    @FXML private var clearLogBtn: Button? = null
    @FXML private var connectBtn: Button? = null
    @FXML private var disconnectBtn: Button? = null
    
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        setupInitialValues()
        setupEventHandlers()
        createInitialPoint()
    }
    
    private fun setupInitialValues() {
        // Set default values for input fields
        compassDirection?.text = "0"
        compassRadius?.text = "100"
        compassHeight?.text = "0"
        azimuthDirection?.text = "0"
        azimuthRadius?.text = "100"
        azimuthHeight?.text = "0"
        coordinateX?.text = "0"
        coordinateY?.text = "0"
        coordinateZ?.text = "0"
        
        // Populate combo boxes
        inputInfoCombo?.items?.addAll(predefinedResults.map { it.name })
        inputInfoCombo?.selectionModel?.selectFirst()
        
        // ComboBox 선택 이벤트 리스너 추가
        inputInfoCombo?.selectionModel?.selectedItemProperty()?.addListener { _, _, newValue ->
            newValue?.let { loadPredefinedData(it) }
        }
        
        // Setup COM port options
        comPortMotorCombo?.let { combo ->
            for (i in 1..20) {
                combo.items.add("COM$i")
            }
            combo.selectionModel.selectFirst()
        }
        
        // Initialize log area
        logTextArea?.text = "Balance Master v2.4.0 시작됨\n시리얼 포트를 선택하고 연결하세요.\n"
    }
    
    private fun createInitialPoint() {
        // 첫 번째 미리 정의된 데이터 로드
        if (predefinedResults.isNotEmpty()) {
            loadPredefinedData(predefinedResults[0].name)
        } else {
            // 기본 Point 하나 생성
            addNewPoint()
        }
    }
    
    private fun createPointUI(pointData: PointData): HBox {
        val pointRow = HBox().apply {
            spacing = 5.0
            prefHeight = 35.0
            prefWidth = 515.0
        }
        
        // Point 이름 라벨
        val pointLabel = Label(pointData.pointName).apply {
            prefHeight = 35.0
            prefWidth = 70.0
            style = "-fx-text-fill: white; -fx-alignment: center;"
            font = Font("Arial Narrow Bold", 16.0)
        }
        
        // X 입력 필드
        val xField = TextField().apply {
            prefHeight = 30.0
            prefWidth = 85.0
            text = pointData.x.toString()
            style = "-fx-alignment: center;"
            font = Font("Arial", 14.0)
        }
        
        // Y 입력 필드
        val yField = TextField().apply {
            prefHeight = 30.0
            prefWidth = 85.0
            text = pointData.y.toString()
            style = "-fx-alignment: center;"
            font = Font("Arial", 14.0)
        }
        
        // Z 입력 필드
        val zField = TextField().apply {
            prefHeight = 30.0
            prefWidth = 85.0
            text = pointData.z.toString()
            style = "-fx-alignment: center;"
            font = Font("Arial", 14.0)
        }
        
        // Deviation 입력 필드
        val deviField = TextField().apply {
            prefHeight = 30.0
            prefWidth = 85.0
            text = "0.000"
            style = "-fx-alignment: center; -fx-text-fill: red;"
            font = Font("Arial", 14.0)
            isEditable = false
        }
        
        // 값 변경 이벤트 리스너 추가
        xField.textProperty().addListener { _, _, newValue ->
            try {
                pointData.x = newValue.toDoubleOrNull() ?: 0.0
                calculateDeviation(pointData, deviField)
            } catch (e: Exception) {
                logMessage("X 값 오류: ${e.message}")
            }
        }
        
        yField.textProperty().addListener { _, _, newValue ->
            try {
                pointData.y = newValue.toDoubleOrNull() ?: 0.0
                calculateDeviation(pointData, deviField)
            } catch (e: Exception) {
                logMessage("Y 값 오류: ${e.message}")
            }
        }
        
        zField.textProperty().addListener { _, _, newValue ->
            try {
                pointData.z = newValue.toDoubleOrNull() ?: 0.0
                calculateDeviation(pointData, deviField)
            } catch (e: Exception) {
                logMessage("Z 값 오류: ${e.message}")
            }
        }
        
        pointRow.children.addAll(pointLabel, xField, yField, zField, deviField)
        HBox.setMargin(pointLabel, Insets(2.5, 5.0, 2.5, 15.0))
        HBox.setMargin(xField, Insets(2.5, 5.0, 2.5, 5.0))
        HBox.setMargin(yField, Insets(2.5, 5.0, 2.5, 5.0))
        HBox.setMargin(zField, Insets(2.5, 5.0, 2.5, 5.0))
        HBox.setMargin(deviField, Insets(2.5, 5.0, 2.5, 5.0))
        
        return pointRow
    }
    
    private fun calculateDeviation(pointData: PointData, deviField: TextField) {
        // 편차는 보통 0.000으로 표시 (특별한 경우에만 계산됨)
        pointData.deviation = 0.0
        deviField.text = "0.000"
    }
    
    private fun addNewPoint() {
        val newPoint = PointData("P$pointCounter", 0.0, 0.0, 0.0, 0.0)
        pointList.add(newPoint)
        
        val pointUI = createPointUI(newPoint)
        pointUIList.add(pointUI)
        
        dynamicContainer?.children?.add(pointUI)
        
        pointCounter++
        logMessage("새 포인트 P${pointCounter-1}이 추가되었습니다")
    }
    
    private fun removeLastPoint() {
        if (pointList.size > 1) { // 최소 1개는 유지
            val removedPoint = pointList.removeLastOrNull()
            val removedUI = pointUIList.removeLastOrNull()
            
            removedUI?.let { 
                dynamicContainer?.children?.remove(it)
            }
            
            pointCounter--
            logMessage("포인트 ${removedPoint?.pointName}이 제거되었습니다")
        } else {
            logMessage("최소 1개의 포인트는 유지되어야 합니다")
        }
    }
    
    private fun loadPredefinedData(dataName: String) {
        val resultData = predefinedResults.find { it.name == dataName }
        if (resultData == null) {
            logMessage("데이터를 찾을 수 없습니다: $dataName")
            return
        }
        
        // 기존 포인트들 모두 제거
        clearAllPoints()
        
        // 새로운 포인트들 추가
        resultData.points.forEach { pointData ->
            val newPoint = PointData(pointData.pointName, pointData.x, pointData.y, pointData.z, pointData.deviation)
            pointList.add(newPoint)
            
            val pointUI = createPointUI(newPoint)
            pointUIList.add(pointUI)
            dynamicContainer?.children?.add(pointUI)
        }
        
        pointCounter = resultData.points.size + 1
        
        // 결과값들 업데이트
        updateResultLabels(resultData)
        
        // Wheel Position 업데이트
        updateWheelPositions(resultData)
        
        // Product 정보 업데이트
        updateProductInfo(resultData)
        
        logMessage("데이터셋 '${resultData.name}'이 로드되었습니다")
    }
    
    private fun clearAllPoints() {
        pointList.clear()
        pointUIList.clear()
        dynamicContainer?.children?.clear()
        pointCounter = 1
    }
    
    private fun updateResultLabels(resultData: ResultData) {
        // B_P 관련 값들
        bRpDirLabel?.text = String.format("%.3f", resultData.bpDirection)
        bRpAngLabel?.text = String.format("%.4f", resultData.bpAngle)
        
        // A_P 관련 값들
        aRpDirLabel?.text = String.format("%.3f", resultData.apDirection)
        aRpAngLabel?.text = String.format("%.4f", resultData.apAngle)
        
        // Center Shift 값들
        shiftXLabel?.text = String.format("%.3f", resultData.centerShiftX)
        shiftYLabel?.text = String.format("%.3f", resultData.centerShiftY)
        shiftZLabel?.text = String.format("%.3f", resultData.centerShiftZ)
    }
    
    private fun updateWheelPositions(resultData: ResultData) {
        // 1st Wh, 2nd Wh 결과값들
        firstWheelText?.text = String.format("%.3f°", resultData.firstWheel)
        secondWheelText?.text = String.format("%.3f°", resultData.secondWheel)
        
        // 빨간색 마이너스 값들
        firstMinusWheelText?.text = String.format("%.3f°", resultData.firstWheelMinus)
        secondMinusWheelText?.text = String.format("%.3f°", resultData.secondWheelMinus)
        
        // Wheel Position 입력값들 (0으로 고정)
        oldFirstWheelPos?.text = "0"
        oldSecondWheelPos?.text = "0"
    }
    
    private fun updateProductInfo(resultData: ResultData) {
        // Product 정보 업데이트
        inAngleMax?.text = String.format("%.1f°", resultData.angleTracking)
        productCodeLabel?.text = resultData.productCode
        nameOfRecipe?.text = resultData.recipe
    }
    
    private fun setupEventHandlers() {
        // Page navigation
        mainPageBtn?.setOnAction { showMainPage() }
        recipePageBtn?.setOnAction { showRecipePage() }
        ioCommPageBtn?.setOnAction { showIOCommPage() }
        optionPageBtn?.setOnAction { showOptionPage() }
        
        // Point management
        addPointInputBtn?.setOnAction { addPoint() }
        deletePointInputBtn?.setOnAction { deletePoint() }
        savePointsInfoBtn?.setOnAction { savePoints() }
        
        // Communication
        connectBtn?.setOnAction { connectToPort() }
        disconnectBtn?.setOnAction { disconnectFromPort() }
        sendMessageBtn?.setOnAction { sendMessage() }
        clearLogBtn?.setOnAction { clearLog() }
    }
    
    // Navigation methods
    private fun showMainPage() {
        pageMainPane?.isVisible = true
        pageRecipePane?.isVisible = false
        pageIOCommPane?.isVisible = false
        pageOptionPane?.isVisible = false
        logMessage("메인 페이지로 이동")
    }
    
    private fun showRecipePage() {
        pageMainPane?.isVisible = false
        pageRecipePane?.isVisible = true
        pageIOCommPane?.isVisible = false
        pageOptionPane?.isVisible = false
        logMessage("레시피 페이지로 이동")
    }
    
    private fun showIOCommPage() {
        pageMainPane?.isVisible = false
        pageRecipePane?.isVisible = false
        pageIOCommPane?.isVisible = true
        pageOptionPane?.isVisible = false
        logMessage("I/O 통신 페이지로 이동")
    }
    
    private fun showOptionPage() {
        pageMainPane?.isVisible = false
        pageRecipePane?.isVisible = false
        pageIOCommPane?.isVisible = false
        pageOptionPane?.isVisible = true
        logMessage("옵션 페이지로 이동")
    }
    
    // Point management methods
    private fun addPoint() {
        addNewPoint()
    }
    
    private fun deletePoint() {
        removeLastPoint()
    }
    
    private fun savePoints() {
        val selectedInfo = inputInfoCombo?.selectionModel?.selectedItem ?: "기본"
        logMessage("포인트 정보가 '$selectedInfo'로 저장되었습니다")
        logMessage("저장된 포인트 수: ${pointList.size}")
        
        // 현재 결과값들도 로그 출력
        val currentResult = predefinedResults.find { it.name == selectedInfo }
        currentResult?.let { result ->
            logMessage("=== 결과 정보 ===")
            logMessage("B_P Direction: ${result.bpDirection}, Angle: ${result.bpAngle}")
            logMessage("A_P Direction: ${result.apDirection}, Angle: ${result.apAngle}")
            logMessage("Center Shift - X: ${result.centerShiftX}, Y: ${result.centerShiftY}, Z: ${result.centerShiftZ}")
            logMessage("1st Wheel: ${result.firstWheel}° (${result.firstWheelMinus}°), 2nd Wheel: ${result.secondWheel}° (${result.secondWheelMinus}°)")
            logMessage("Product Code: ${result.productCode}, Recipe: ${result.recipe}")
        }
        
        // 각 포인트 정보 로그 출력
        pointList.forEachIndexed { index, point ->
            logMessage("${point.pointName}: X=${point.x}, Y=${point.y}, Z=${point.z}, Devi=${String.format("%.3f", point.deviation)}")
        }
    }
    
    // Communication methods
    private fun connectToPort() {
        val selectedPort = comPortMotorCombo?.selectionModel?.selectedItem
        if (selectedPort != null) {
            logMessage("$selectedPort 에 연결 시도 중...")
            // TODO: Implement actual serial connection
            logMessage("$selectedPort 연결 성공")
        } else {
            logMessage("포트를 선택해주세요")
        }
    }
    
    private fun disconnectFromPort() {
        logMessage("포트 연결이 해제되었습니다")
        // TODO: Implement actual disconnection
    }
    
    private fun sendMessage() {
        logMessage("메시지를 전송했습니다")
        // TODO: Implement message sending
    }
    
    private fun clearLog() {
        logTextArea?.clear()
        logMessage("Balance Master v2.4.0 - 로그 초기화됨")
    }
    
    private fun logMessage(message: String) {
        val timestamp = java.time.LocalTime.now().toString().substring(0, 8)
        logTextArea?.appendText("[$timestamp] $message\n")
    }
    
    // Event handlers for FXML
    @FXML
    private fun onCompassCalculate(event: ActionEvent) {
        val direction = compassDirection?.text?.toDoubleOrNull() ?: 0.0
        val radius = compassRadius?.text?.toDoubleOrNull() ?: 0.0
        val height = compassHeight?.text?.toDoubleOrNull() ?: 0.0
        
        // Convert compass input to coordinates
        val x = radius * kotlin.math.cos(Math.toRadians(direction))
        val y = radius * kotlin.math.sin(Math.toRadians(direction))
        val z = height
        
        coordinateX?.text = String.format("%.2f", x)
        coordinateY?.text = String.format("%.2f", y)
        coordinateZ?.text = String.format("%.2f", z)
        
        logMessage("나침반 좌표 계산 완료: 방향=$direction°, 반지름=$radius")
    }
    
    @FXML
    private fun onAzimuthCalculate(event: ActionEvent) {
        val direction = azimuthDirection?.text?.toDoubleOrNull() ?: 0.0
        val radius = azimuthRadius?.text?.toDoubleOrNull() ?: 0.0
        val height = azimuthHeight?.text?.toDoubleOrNull() ?: 0.0
        
        // Convert azimuth input to coordinates
        val x = radius * kotlin.math.cos(Math.toRadians(90 - direction))
        val y = radius * kotlin.math.sin(Math.toRadians(90 - direction))
        val z = height
        
        coordinateX?.text = String.format("%.2f", x)
        coordinateY?.text = String.format("%.2f", y)
        coordinateZ?.text = String.format("%.2f", z)
        
        logMessage("방위각 좌표 계산 완료: 방위각=$direction°, 반지름=$radius")
    }
    
    // Additional FXML event handlers
    @FXML
    private fun onAddPointInputBtnClick(event: ActionEvent) {
        addPoint()
    }
    
    @FXML
    private fun onDeletePointInputBtnClick(event: ActionEvent) {
        deletePoint()
    }
    
    @FXML
    private fun onSavePointsInfoClick(event: ActionEvent) {
        savePoints()
    }
    
    @FXML
    private fun onDeletePointsInfoClick(event: ActionEvent) {
        logMessage("포인트 정보가 삭제되었습니다")
    }
    
    @FXML
    private fun onResultTextRadioBtnClick(event: ActionEvent) {
        logMessage("텍스트 결과 모드로 변경")
    }
    
    @FXML
    private fun onResultGraphRadioBtnClick(event: ActionEvent) {
        logMessage("그래프 결과 모드로 변경")
    }
    
    @FXML
    private fun onInitButtonClick(event: ActionEvent) {
        // Reset all values
        compassDirection?.text = "0"
        compassRadius?.text = "100"
        compassHeight?.text = "0"
        azimuthDirection?.text = "0"
        azimuthRadius?.text = "0"
        azimuthHeight?.text = "0"
        coordinateX?.text = "0"
        coordinateY?.text = "0"
        coordinateZ?.text = "0"
        logMessage("모든 값이 초기화되었습니다")
    }
    
    @FXML
    private fun onFindButtonClick(event: ActionEvent) {
        logMessage("포인트 검색을 시작합니다")
        // TODO: Implement point finding algorithm
    }
    
    @FXML
    private fun onThreeDButtonClick(event: ActionEvent) {
        logMessage("3D 뷰어를 실행합니다")
        openThreeDViewer()
    }
    
    private fun openThreeDViewer() {
        try {
            val fxmlLoader = FXMLLoader(javaClass.getResource("/threeDViewerSimple.fxml"))
            
            // Use Spring context to create controller
            fxmlLoader.setControllerFactory { clazz ->
                com.example.BalanceStage.BalanceStageApplication.applicationContext.getBean(clazz)
            }
            
            val scene = Scene(fxmlLoader.load(), 900.0, 700.0)
            val stage = Stage()
            stage.title = "3D Model Viewer - Balance Master (FXyz3D)"
            stage.scene = scene
            stage.show()
            
            logMessage("FXyz3D 기반 3D 뷰어 창이 열렸습니다")
            
        } catch (e: Exception) {
            logMessage("3D 뷰어 열기 실패: ${e.message}")
            e.printStackTrace()
        }
    }
    
    @FXML
    private fun onMoveRecipeButtonClick(event: ActionEvent) {
        showRecipePage()
    }
    
    @FXML
    private fun onMoveMainButtonClick(event: ActionEvent) {
        showMainPage()
    }
    
    @FXML
    private fun onMoveMotionButtonClick(event: ActionEvent) {
        showOptionPage()
    }
    
    @FXML
    private fun onMoveIOCommButtonClick(event: ActionEvent) {
        showIOCommPage()
    }
    
    @FXML
    private fun onShowButtonClick(event: ActionEvent) {
        logMessage("화면 표시 옵션이 변경되었습니다")
    }
    
    @FXML
    private fun onLogButtonClick(event: ActionEvent) {
        logMessage("로그 설정이 변경되었습니다")
    }
    
    @FXML
    private fun onExitButtonClick(event: ActionEvent) {
        logMessage("애플리케이션을 종료합니다")
        // Platform.exit()
    }
    
    @FXML
    private fun onLogTextClearBtn(event: ActionEvent) {
        clearLog()
    }
    
    // Recipe management
    @FXML
    private fun onRecipeAddBtnClick(event: ActionEvent) {
        logMessage("새 레시피가 추가되었습니다")
    }
    
    @FXML
    private fun onRecipeSaveBtnClick(event: ActionEvent) {
        logMessage("레시피가 저장되었습니다")
    }
    
    @FXML
    private fun onRecipeDeleteBtnClick(event: ActionEvent) {
        logMessage("레시피가 삭제되었습니다")
    }
    
    @FXML
    private fun onFarAwayButtonClick(event: ActionEvent) {
        logMessage("Z+ 방향: 멀어지기 선택됨")
    }
    
    @FXML
    private fun onCloserButtonClick(event: ActionEvent) {
        logMessage("Z+ 방향: 가까워지기 선택됨")
    }
    
    // IO Communication
    @FXML
    private fun onGyroCheckBtnClick(event: ActionEvent) {
        logMessage("자이로 센서 체크를 시작합니다")
    }
    
    // Option/Motion controls
    @FXML
    private fun onOptionWheelSetLoad(event: ActionEvent) {
        logMessage("휠 설정을 로드했습니다")
    }
    
    @FXML
    private fun onJogFirstForwardBtn(event: ActionEvent) {
        logMessage("1번 휠 전진")
    }
    
    @FXML
    private fun onJogFirstBackwardBtn(event: ActionEvent) {
        logMessage("1번 휠 후진")
    }
    
    @FXML
    private fun onJogSecondForwardBtn(event: ActionEvent) {
        logMessage("2번 휠 전진")
    }
    
    @FXML
    private fun onJogSecondBackwardBtn(event: ActionEvent) {
        logMessage("2번 휠 후진")
    }
    
    @FXML
    private fun onJogStatusBtn(event: ActionEvent) {
        logMessage("모터 상태를 확인합니다")
    }
    
    @FXML
    private fun onJogAllGoHomeBtn(event: ActionEvent) {
        logMessage("모든 모터가 홈 위치로 이동합니다")
    }
    
    @FXML
    private fun onJogMoveToBtn(event: ActionEvent) {
        logMessage("지정된 위치로 이동합니다")
    }
    
    @FXML
    private fun onJogShuttleBtn(event: ActionEvent) {
        logMessage("셔틀 동작을 실행합니다")
    }
    
    @FXML
    private fun onJogCopyFindBtn(event: ActionEvent) {
        logMessage("찾기 결과를 복사합니다")
    }
    
    @FXML
    private fun onOptionWheelSetSaveBtn(event: ActionEvent) {
        logMessage("휠 설정이 저장되었습니다")
    }
    
    @FXML
    private fun onOptionWheelSetSendBtn(event: ActionEvent) {
        logMessage("휠 설정을 전송했습니다")
    }
    
    @FXML
    private fun onOptionSetHomeBtn(event: ActionEvent) {
        logMessage("홈 위치가 설정되었습니다")
    }
    
    @FXML
    private fun onTextSendToBoard(event: ActionEvent) {
        logMessage("보드에 텍스트를 전송했습니다")
    }
} 