# 2D 등고선 시각화 모듈 (Contour Visualization Module)

JavaFX Canvas 기반 2D 등고선 렌더링 엔진입니다. 3D 측정 포인트의 편차값을 2D 등고선 맵으로 시각화합니다.

## 📦 패키지 구조

```
com.example.BalanceStage.visualization/
├── ContourRenderer.kt      # 메인 렌더링 엔진
├── ContourConfig.kt        # 렌더링 설정 클래스
├── ColorMap.kt             # 컬러맵 Enum
└── README.md              # 이 파일
```

## 🚀 빠른 시작

### 기본 사용법

```kotlin
import com.example.BalanceStage.visualization.ContourRenderer
import com.example.BalanceStage.visualization.ContourConfig
import com.example.BalanceStage.visualization.ColorMap

// 1. 측정 데이터 준비 (x좌표, y좌표, z편차값)
val points = listOf(
    Triple(0.0, 0.0, 0.005),      // P1
    Triple(10.0, 0.0, -0.003),    // P2
    Triple(10.0, 10.0, 0.002),    // P3
    Triple(0.0, 10.0, -0.001)     // P4
)

// 2. 렌더러 생성 (캔버스 크기 지정)
val renderer = ContourRenderer(width = 520.0, height = 420.0)

// 3. 렌더링 (기본 설정 사용)
val canvas = renderer.render(points)

// 4. UI에 추가
myPane.children.add(canvas)
```

### 설정 커스터마이징

```kotlin
// 커스텀 설정으로 렌더링
val config = ContourConfig(
    gridSize = 160,              // 중간 품질
    numContours = 16,            // 16개 등고선
    sigma = 0.3,                 // 중간 스무딩
    colorMap = ColorMap.SPECTRAL // Spectral 컬러맵
)

val canvas = renderer.render(points, config)
```

### 미리 정의된 설정 사용

```kotlin
// 실시간 시뮬레이션용 (빠름)
val canvas = renderer.render(points, ContourConfig.REALTIME)

// 중간 품질 (균형)
val canvas = renderer.render(points, ContourConfig.BALANCED)

// 고품질 출력용 (느리지만 최고 품질)
val canvas = renderer.render(points, ContourConfig.HIGH_QUALITY)
```

### 기존 Canvas에 렌더링

```kotlin
// Canvas 재사용 시
val existingCanvas = Canvas(520.0, 420.0)
val gc = existingCanvas.graphicsContext2D

renderer.renderToGraphicsContext(gc, points, config)
```

---

## 📥 INPUT 정의

### 입력 데이터 형식

```kotlin
val points: List<Triple<Double, Double, Double>>
//              ↑       ↑       ↑
//              X좌표   Y좌표   Z편차값
```

**요구사항:**
- 최소 1개 이상의 포인트 필요 (권장: 3개 이상)
- 좌표 단위는 임의 (mm, cm, inch 등)
- 편차값 단위도 임의 (보통 mm)

**예제 데이터:**

```kotlin
// 4개 측정점 예제 (단위: mm)
val measurements = listOf(
    Triple(5.0, 5.0, 0.010),      // 10μm 양의 편차
    Triple(15.0, 5.0, -0.005),    // 5μm 음의 편차
    Triple(15.0, 15.0, 0.003),    // 3μm 양의 편차
    Triple(5.0, 15.0, -0.002)     // 2μm 음의 편차
)
```

---

## ⚙️ 설정 옵션 (ContourConfig)

### ContourConfig 파라미터

| 파라미터 | 타입 | 기본값 | 범위 | 설명 |
|---------|------|-------|------|------|
| `gridSize` | Int | 260 | 60~260 | 그리드 해상도 (높을수록 부드럽지만 느림) |
| `numContours` | Int | 24 | 8~24 | 등고선 개수 |
| `sigma` | Double | 0.50 | 0.0~0.5 | 가우시안 스무딩 강도 |
| `colorMap` | ColorMap | TURBO | - | 컬러맵 종류 |
| `neutralDeviationThreshold` | Double | 0.01 | ≥0.0 | 중립 색상 압축 임계값 |

### gridSize (그리드 해상도)

그리드 해상도는 렌더링 품질과 속도에 직접적인 영향을 미칩니다.

| 값 | 셀 개수 | 렌더링 시간 | 용도 |
|----|---------|------------|------|
| 60 | 3,600 | ~10ms | 실시간 시뮬레이션 |
| 160 | 25,600 | ~50ms | 일반 사용 |
| 260 | 67,600 | ~100ms | 고품질 출력 |

```kotlin
// 실시간 시뮬레이션 (빠름)
val config = ContourConfig(gridSize = 60)

// 최종 결과 표시 (고품질)
val config = ContourConfig(gridSize = 260)
```

### numContours (등고선 개수)

등고선 개수가 많을수록 더 세밀한 변화를 확인할 수 있습니다.

```kotlin
// 간단한 시각화
val config = ContourConfig(numContours = 8)

// 상세한 등고선
val config = ContourConfig(numContours = 24)
```

### sigma (가우시안 스무딩)

노이즈 제거 및 부드러운 등고선을 위한 스무딩 강도입니다.

```kotlin
// 스무딩 없음 (원본 데이터)
val config = ContourConfig(sigma = 0.0)

// 중간 스무딩
val config = ContourConfig(sigma = 0.3)

// 강한 스무딩 (노이즈 많은 데이터)
val config = ContourConfig(sigma = 0.5)
```

### neutralDeviationThreshold (중립 색상 압축)

작은 편차를 시각적으로 강조하기 위한 파라미터입니다.

```kotlin
// 압축 없음 (전체 범위 사용)
val config = ContourConfig(neutralDeviationThreshold = 0.0)

// 0.01mm 미만 편차 강조
val config = ContourConfig(neutralDeviationThreshold = 0.01)
```

**작동 원리:**
- 전체 편차 범위가 임계값보다 작으면 중립 색상 근처로 압축
- 작은 변화를 더 명확하게 시각화 가능

---

## 🎨 컬러맵 (ColorMap)

6가지 컬러맵을 지원합니다.

### ColorMap.TURBO (권장)
- Google Turbo 컬러맵
- 지각적으로 균일하고 색맹 친화적
- **권장 용도:** 일반적인 데이터 시각화

```kotlin
val config = ContourConfig(colorMap = ColorMap.TURBO)
```

### ColorMap.JET
- MATLAB Jet 컬러맵 (파랑-청록-초록-노랑-빨강)
- 전통적인 과학/공학 분야에서 사용
- **권장 용도:** 전통적인 과학 논문

```kotlin
val config = ContourConfig(colorMap = ColorMap.JET)
```

### ColorMap.VIRIDIS
- 보라-파랑-초록-노랑 그라데이션
- 지각적으로 균일하고 색맹 친화적
- **권장 용도:** 출판물, 프레젠테이션

```kotlin
val config = ContourConfig(colorMap = ColorMap.VIRIDIS)
```

### ColorMap.COOLWARM
- 파랑-흰색-빨강 발산형
- 중간값 강조, 양극단 명확히 구분
- **권장 용도:** 편차 분석, 중립값 중심 데이터

```kotlin
val config = ContourConfig(colorMap = ColorMap.COOLWARM)
```

### ColorMap.SPECTRAL
- 빨강-주황-노랑-초록-파랑-보라 스펙트럼
- 전통적인 등고선 지도 스타일
- **권장 용도:** 지형도, 등고선 맵

```kotlin
val config = ContourConfig(colorMap = ColorMap.SPECTRAL)
```

### ColorMap.RDYLBU
- 진한 빨강-빨강-노랑-연한 파랑-진한 파랑
- 기상도/지형도 스타일
- **권장 용도:** 온도, 고도, 압력 등

```kotlin
val config = ContourConfig(colorMap = ColorMap.RDYLBU)
```

---

## 📤 OUTPUT 정의

### 반환 타입

```kotlin
// 방법 1: Canvas 객체 반환
fun render(
    points: List<Triple<Double, Double, Double>>,
    config: ContourConfig = ContourConfig()
): Canvas

// 방법 2: 기존 GraphicsContext에 렌더링 (void)
fun renderToGraphicsContext(
    gc: GraphicsContext,
    points: List<Triple<Double, Double, Double>>,
    config: ContourConfig = ContourConfig()
)
```

### 출력 내용

렌더링된 Canvas는 다음 요소를 포함합니다:

1. **컬러 그리드:** IDW 보간 + 가우시안 스무딩으로 생성된 배경
2. **등고선:** Marching Squares 알고리즘으로 추출된 검은색 선
3. **측정점 라벨:** P1, P2, P3... 형태의 텍스트 라벨
4. **컬러바:** 우측에 값 범위를 표시하는 컬러 범례
5. **여백:** 30px 내부 여백

---

## 🔧 고급 사용법

### 동적 설정 변경

```kotlin
// 사용자가 슬라이더로 설정 조절
slider.valueProperty().addListener { _, _, newValue ->
    val config = ContourConfig(
        gridSize = newValue.toInt(),
        colorMap = colorComboBox.value
    )
    val newCanvas = renderer.render(points, config)

    // Canvas 교체
    canvasContainer.children.clear()
    canvasContainer.children.add(newCanvas)
}
```

### 실시간 시뮬레이션 예제

```kotlin
// 시뮬레이션 중 실시간 업데이트
val renderer = ContourRenderer(520.0, 420.0)
val canvas = Canvas(520.0, 420.0)
val gc = canvas.graphicsContext2D

// 타이머로 주기적 업데이트
val timeline = Timeline(KeyFrame(Duration.millis(16.6)) {
    // 포인트 업데이트
    updateSimulation()

    // 빠른 설정으로 재렌더링
    renderer.renderToGraphicsContext(gc, currentPoints, ContourConfig.REALTIME)
})
timeline.cycleCount = Timeline.INDEFINITE
timeline.play()
```

### 데이터 없을 때 처리

```kotlin
val points = emptyList<Triple<Double, Double, Double>>()
val canvas = renderer.render(points)  // "데이터 없음" 메시지 표시
```

---

## 📊 성능 특성

### 렌더링 시간 (참고)

| gridSize | 셀 개수 | 예상 시간 | 프레임레이트 |
|----------|---------|----------|-------------|
| 60 | 3,600 | ~10ms | 100 fps |
| 100 | 10,000 | ~25ms | 40 fps |
| 160 | 25,600 | ~50ms | 20 fps |
| 260 | 67,600 | ~100ms | 10 fps |

**최적화 팁:**
- 실시간 업데이트: `gridSize = 60`
- 정적 표시: `gridSize = 260`
- Canvas 재사용: `renderToGraphicsContext()` 메서드 사용

---

## 🎯 실전 예제

### 예제 1: 기본 사용

```kotlin
import com.example.BalanceStage.visualization.*

fun displayContour() {
    val points = listOf(
        Triple(0.0, 0.0, 0.005),
        Triple(10.0, 0.0, -0.003),
        Triple(10.0, 10.0, 0.002),
        Triple(0.0, 10.0, -0.001)
    )

    val renderer = ContourRenderer(520.0, 420.0)
    val canvas = renderer.render(points, ContourConfig.HIGH_QUALITY)

    myPane.children.add(canvas)
}
```

### 예제 2: 사용자 설정 UI

```kotlin
import com.example.BalanceStage.visualization.*
import javafx.scene.control.*

fun createConfigUI() {
    val gridSizeSlider = Slider(60.0, 260.0, 160.0)
    val contourSlider = Slider(8.0, 24.0, 16.0)
    val sigmaSlider = Slider(0.0, 0.5, 0.3)
    val colorMapComboBox = ComboBox<ColorMap>().apply {
        items.addAll(ColorMap.values())
        value = ColorMap.TURBO
    }

    val updateButton = Button("업데이트").apply {
        setOnAction {
            val config = ContourConfig(
                gridSize = gridSizeSlider.value.toInt(),
                numContours = contourSlider.value.toInt(),
                sigma = sigmaSlider.value,
                colorMap = colorMapComboBox.value
            )

            val canvas = renderer.render(points, config)
            canvasPane.children.setAll(canvas)
        }
    }
}
```

### 예제 3: 여러 컬러맵 비교

```kotlin
import com.example.BalanceStage.visualization.*

fun compareColorMaps(points: List<Triple<Double, Double, Double>>) {
    val renderer = ContourRenderer(300.0, 300.0)
    val colorMaps = ColorMap.values()

    val hbox = HBox(10.0)
    colorMaps.forEach { colorMap ->
        val config = ContourConfig(
            gridSize = 100,
            colorMap = colorMap
        )
        val canvas = renderer.render(points, config)

        val vbox = VBox(5.0).apply {
            children.addAll(
                Label(colorMap.name),
                canvas
            )
        }
        hbox.children.add(vbox)
    }

    myPane.children.add(hbox)
}
```

---

## 🧪 알고리즘 상세

### 1. IDW 보간 (Inverse Distance Weighting)

측정점 사이의 값을 추정하는 방법입니다.

**수식:**
```
weight = 1 / (distance² + ε)
interpolated_value = Σ(weight × value) / Σ(weight)
```

**특징:**
- 측정점에서 정확히 원값 반환
- 거리의 제곱에 반비례하는 가중치
- 안정성을 위한 ε = 1e-6

### 2. 가우시안 스무딩

3×3 커널을 사용한 컨볼루션으로 노이즈를 제거합니다.

**커널 생성:**
```
kernel[i][j] = exp(-(i² + j²) / (2σ²))
```

**정규화:** 커널 합계 = 1

### 3. Marching Squares

등고선을 추출하는 표준 알고리즘입니다.

**원리:**
- 2×2 그리드 셀의 4개 코너 값 확인
- 16가지 케이스에 따라 선분 그리기
- 선형 보간으로 정확한 교점 계산

---

## 🔍 문제 해결

### Q: 등고선이 너무 거칠어요
**A:** `gridSize`를 증가시키거나 `sigma` 값을 높여보세요.

```kotlin
val config = ContourConfig(
    gridSize = 260,  // 더 높은 해상도
    sigma = 0.5      // 더 강한 스무딩
)
```

### Q: 렌더링이 너무 느려요
**A:** `gridSize`를 낮추세요.

```kotlin
val config = ContourConfig.REALTIME  // gridSize = 60
```

### Q: 색상 대비가 약해요
**A:** `neutralDeviationThreshold`를 조절하거나 다른 컬러맵을 시도하세요.

```kotlin
val config = ContourConfig(
    neutralDeviationThreshold = 0.005,  // 더 낮은 값
    colorMap = ColorMap.COOLWARM        // 발산형 컬러맵
)
```

### Q: 측정점 라벨이 안 보여요
**A:** 라벨은 항상 표시됩니다. 배경색과 대비를 위해 흰색 외곽선 + 검은색 텍스트를 사용합니다.

---

## 📝 의존성

**필수 의존성:**
- JavaFX 17.0.2 이상
- Kotlin 1.9.25 이상

**선택 의존성:**
- 없음 (완전히 독립적인 모듈)
