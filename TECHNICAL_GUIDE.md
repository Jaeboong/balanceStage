# 2D 등고선 시각화 모듈 기술 문서

**모듈명:** Contour Visualization Module
**버전:** 1.0
**언어:** Kotlin 1.9.25
**프레임워크:** JavaFX 17.0.2
**패키지:** com.example.BalanceStage.visualization

---

## 목차

1. [설치 및 설정](#1-설치-및-설정)
2. [실행 방법](#2-실행-방법)
3. [API 레퍼런스](#3-api-레퍼런스)
4. [설정 옵션](#4-설정-옵션)
5. [사용 예제](#5-사용-예제)
6. [성능 가이드](#6-성능-가이드)
7. [문제 해결](#7-문제-해결)

---

## 1. 설치 및 설정

### 1.1 의존성 추가

#### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("org.openjfx:javafx-graphics:17.0.2")
    implementation("org.openjfx:javafx-controls:17.0.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.25")
}
```

#### Gradle (Groovy)

```groovy
dependencies {
    implementation 'org.openjfx:javafx-graphics:17.0.2'
    implementation 'org.openjfx:javafx-controls:17.0.2'
    implementation 'org.jetbrains.kotlin:kotlin-stdlib:1.9.25'
}
```

#### Maven

```xml
<dependencies>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-graphics</artifactId>
        <version>17.0.2</version>
    </dependency>
</dependencies>
```

### 1.2 모듈 복사

visualization 폴더 전체를 프로젝트로 복사함:

```bash
# 소스 복사
cp -r visualization/ /your/project/src/main/kotlin/

# 필요시 패키지명 변경
```

### 1.3 Import 구문

```kotlin
import com.example.BalanceStage.visualization.ContourRenderer
import com.example.BalanceStage.visualization.ContourConfig
import com.example.BalanceStage.visualization.ColorMap
```

---

## 2. 실행 방법

### 2.1 기본 실행

```kotlin
// 측정 데이터 준비
val points = listOf(
    Triple(0.0, 0.0, 0.005),
    Triple(10.0, 0.0, -0.003),
    Triple(10.0, 10.0, 0.002)
)

// 렌더러 생성 및 렌더링
val renderer = ContourRenderer(520.0, 420.0)
val canvas = renderer.render(points)

// UI에 표시
myPane.children.add(canvas)
```

### 2.2 프리셋 사용 실행

```kotlin
// 고품질 렌더링
val canvas = renderer.render(points, ContourConfig.HIGH_QUALITY)

// 실시간 렌더링
val canvas = renderer.render(points, ContourConfig.REALTIME)
```

### 2.3 Canvas 재사용 실행

```kotlin
val canvas = Canvas(520.0, 420.0)
val gc = canvas.graphicsContext2D

// 업데이트 함수
fun update(newPoints: List<Triple<...>>) {
    gc.clearRect(0.0, 0.0, 520.0, 420.0)
    renderer.renderToGraphicsContext(gc, newPoints, config)
}
```

---

## 3. API 레퍼런스

### 3.1 ContourRenderer 클래스

#### 생성자

```kotlin
ContourRenderer(
    width: Double = 520.0,    // Canvas 너비
    height: Double = 420.0    // Canvas 높이
)
```

**파라미터:**
- `width`: Canvas 너비 (픽셀 단위)
- `height`: Canvas 높이 (픽셀 단위)

#### render 메서드

```kotlin
fun render(
    points: List<Triple<Double, Double, Double>>,
    config: ContourConfig = ContourConfig()
): Canvas
```

**파라미터:**
- `points`: 측정 포인트 리스트 (x좌표, y좌표, z편차값)
- `config`: 렌더링 설정 (선택사항)

**반환값:** 렌더링된 JavaFX Canvas 객체

**사용 시점:** 새 Canvas 생성이 필요한 경우

#### renderToGraphicsContext 메서드

```kotlin
fun renderToGraphicsContext(
    gc: GraphicsContext,
    points: List<Triple<Double, Double, Double>>,
    config: ContourConfig = ContourConfig()
)
```

**파라미터:**
- `gc`: 렌더링 대상 GraphicsContext
- `points`: 측정 포인트 리스트
- `config`: 렌더링 설정 (선택사항)

**반환값:** 없음 (void)

**사용 시점:** Canvas 재사용이 필요한 경우

### 3.2 ContourConfig 클래스

#### 생성자

```kotlin
data class ContourConfig(
    val gridSize: Int = 260,
    val numContours: Int = 24,
    val sigma: Double = 0.50,
    val colorMap: ColorMap = ColorMap.TURBO,
    val neutralDeviationThreshold: Double = 0.01
)
```

**파라미터:**
- `gridSize`: 그리드 해상도 (10~1000)
- `numContours`: 등고선 개수 (1~100)
- `sigma`: 가우시안 스무딩 강도 (0.0~1.0)
- `colorMap`: 컬러맵 종류
- `neutralDeviationThreshold`: 중립 색상 임계값 (≥0.0)

#### 프리셋 상수

```kotlin
ContourConfig.REALTIME       // 실시간용
ContourConfig.BALANCED       // 균형잡힌 설정
ContourConfig.HIGH_QUALITY   // 고품질용
```

### 3.3 ColorMap Enum

```kotlin
enum class ColorMap {
    TURBO,      // Google Turbo
    JET,        // MATLAB Jet
    VIRIDIS,    // Viridis
    COOLWARM,   // Cool-Warm
    SPECTRAL,   // Spectral
    RDYLBU      // Red-Yellow-Blue
}
```

---

## 4. 설정 옵션

### 4.1 gridSize

그리드 해상도 설정. 높을수록 부드러운 등고선 생성, 렌더링 시간 증가.

**권장값:**
- `60`: 실시간 (~10ms)
- `160`: 일반 사용 (~50ms)
- `260`: 고품질 (~100ms)

**유효 범위:** 10 ~ 1000

```kotlin
val config = ContourConfig(gridSize = 160)
```

### 4.2 numContours

등고선 개수 설정.

**권장값:**
- `8`: 간단한 시각화
- `16`: 일반적 용도
- `24`: 상세한 등고선

**유효 범위:** 1 ~ 100

```kotlin
val config = ContourConfig(numContours = 16)
```

### 4.3 sigma

가우시안 스무딩 강도 설정.

**권장값:**
- `0.0`: 스무딩 없음
- `0.3`: 중간 스무딩
- `0.5`: 강한 스무딩

**유효 범위:** 0.0 ~ 1.0

```kotlin
val config = ContourConfig(sigma = 0.4)
```

### 4.4 colorMap

컬러맵 종류 선택.

**용도별 권장:**
- 일반: TURBO
- 과학 논문: JET
- 출판물: VIRIDIS
- 편차 분석: COOLWARM
- 등고선: SPECTRAL
- 기상도: RDYLBU

```kotlin
val config = ContourConfig(colorMap = ColorMap.SPECTRAL)
```

### 4.5 neutralDeviationThreshold

작은 편차 강조를 위한 압축 임계값 설정.

**권장값:**
- `0.0`: 압축 없음
- `0.005`: 5μm 시스템
- `0.010`: 10μm 시스템

```kotlin
val config = ContourConfig(neutralDeviationThreshold = 0.005)
```

---

## 5. 사용 예제

### 5.1 기본 렌더링

```kotlin
class BasicUsage {
    fun render() {
        val points = listOf(
            Triple(0.0, 0.0, 0.005),
            Triple(10.0, 0.0, -0.003)
        )

        val renderer = ContourRenderer(520.0, 420.0)
        val canvas = renderer.render(points)

        pane.children.add(canvas)
    }
}
```

### 5.2 실시간 업데이트

```kotlin
class RealtimeUpdate {
    private val renderer = ContourRenderer(520.0, 420.0)
    private val canvas = Canvas(520.0, 420.0)

    init {
        pane.children.add(canvas)
    }

    fun update(newPoints: List<Triple<...>>) {
        val gc = canvas.graphicsContext2D
        gc.clearRect(0.0, 0.0, 520.0, 420.0)
        renderer.renderToGraphicsContext(
            gc,
            newPoints,
            ContourConfig.REALTIME
        )
    }
}
```

### 5.3 커스텀 설정

```kotlin
class CustomConfig {
    fun renderCustom() {
        val config = ContourConfig(
            gridSize = 180,
            numContours = 20,
            sigma = 0.4,
            colorMap = ColorMap.VIRIDIS,
            neutralDeviationThreshold = 0.01
        )

        val canvas = renderer.render(points, config)
        display(canvas)
    }
}
```

### 5.4 제품별 설정

```kotlin
// BA2705-05 (5μm 정밀도)
fun renderBA2705() {
    val config = ContourConfig(
        gridSize = 260,
        sigma = 0.3,
        neutralDeviationThreshold = 0.005
    )
    render(config)
}

// BS1501-03 (50μm 정밀도)
fun renderBS1501() {
    val config = ContourConfig(
        gridSize = 100,
        sigma = 0.5,
        neutralDeviationThreshold = 0.050
    )
    render(config)
}
```

### 5.5 센서 연동

```kotlin
class SensorIntegration {
    private val renderer = ContourRenderer(520.0, 420.0)
    private var lastRender = 0L

    fun onData(sensorData: List<Triple<...>>) {
        val now = System.nanoTime()
        if (now - lastRender < 16_666_666L) return

        lastRender = now
        Platform.runLater {
            update(sensorData)
        }
    }

    private fun update(data: List<Triple<...>>) {
        val gc = canvas.graphicsContext2D
        gc.clearRect(0.0, 0.0, 520.0, 420.0)
        renderer.renderToGraphicsContext(gc, data, config)
    }
}
```

---

## 6. 성능 가이드

### 6.1 렌더링 시간

| gridSize | 셀 개수 | 시간 | FPS | 용도 |
|----------|---------|------|-----|------|
| 60 | 3,600 | ~10ms | 100 | 실시간 |
| 160 | 25,600 | ~50ms | 20 | 일반 |
| 260 | 67,600 | ~100ms | 10 | 고품질 |

### 6.2 메모리 최적화

Canvas 재사용 패턴 적용:

```kotlin
// ❌ 비효율적
fun bad(points: List<...>) {
    val canvas = renderer.render(points)
    pane.children.setAll(canvas)
}

// ✅ 효율적
val canvas = Canvas(520.0, 420.0)

fun good(points: List<...>) {
    val gc = canvas.graphicsContext2D
    gc.clearRect(0.0, 0.0, 520.0, 420.0)
    renderer.renderToGraphicsContext(gc, points, config)
}
```

**메모리 절감:** 약 75%

### 6.3 스레드 안전성

JavaFX UI 스레드에서만 호출 필요:

```kotlin
// ✅ 올바른 예
Platform.runLater {
    render(points)
}

// ❌ 잘못된 예
Thread {
    render(points)  // UI 스레드 아님
}.start()
```

### 6.4 업데이트 빈도 제한

```kotlin
private var lastUpdate = 0L
private val INTERVAL = 16_666_666L  // 60fps

fun throttled(points: List<...>) {
    val now = System.nanoTime()
    if (now - lastUpdate < INTERVAL) return

    lastUpdate = now
    render(points)
}
```

---

## 7. 문제 해결

### 7.1 등고선이 거칠게 표시

**원인:** gridSize 낮음 또는 스무딩 부족

**해결:**
```kotlin
val config = ContourConfig(
    gridSize = 260,
    sigma = 0.5
)
```

### 7.2 렌더링 느림

**원인:** gridSize 높음

**해결:**
```kotlin
val config = ContourConfig(gridSize = 60)
```

### 7.3 색상 대비 약함

**원인:** 데이터 범위 좁음 또는 컬러맵 부적합

**해결:**
```kotlin
val config = ContourConfig(
    neutralDeviationThreshold = 0.0,
    colorMap = ColorMap.COOLWARM
)
```

### 7.4 빈 Canvas 표시

**원인:** 빈 데이터 리스트

**확인:**
```kotlin
if (points.isEmpty()) {
    println("Warning: Empty data")
}
```

### 7.5 IllegalArgumentException

**원인:** 유효하지 않은 설정값

**확인:**
```kotlin
try {
    val config = ContourConfig(gridSize = 5000)
} catch (e: IllegalArgumentException) {
    println(e.message)
}
```

### 7.6 "Not on FX thread" 오류

**원인:** UI 스레드 아닌 곳에서 호출

**해결:**
```kotlin
Platform.runLater {
    renderer.render(points)
}
```

### 7.7 메모리 사용량 증가

**원인:** Canvas 매번 생성

**해결:** Canvas 재사용 패턴 적용

---

## 부록

### A. 입력 데이터 형식

```kotlin
List<Triple<Double, Double, Double>>
// Triple<x좌표, y좌표, z편차값>
```

**요구사항:**
- 최소 1개 이상 (권장 3개 이상)
- 좌표 단위: 임의
- 편차 단위: 임의

### B. 출력 구성

1. 컬러 그리드 (IDW 보간 + 가우시안 스무딩)
2. 등고선 (Marching Squares 알고리즘)
3. 측정점 라벨 (P1, P2, P3...)
4. 컬러바 (값 범위 범례)
5. 여백 (30px)

### C. 알고리즘

**IDW 보간:**
```
weight = 1 / (distance² + ε)
value = Σ(weight × value) / Σ(weight)
```

**가우시안 스무딩:**
```
kernel[i][j] = exp(-(i² + j²) / (2σ²))
```

**Marching Squares:**
- 16가지 케이스 처리
- 선형 보간 교점 계산

---

**문서 버전:** 1.0
**최종 수정:** 2025년 1월
