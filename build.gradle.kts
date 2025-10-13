plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
    application
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // ✅ JavaFX (윈도우용). Mac이면 :mac, 리눅스면 :linux 로 바꾸세요.
    implementation("org.openjfx:javafx-base:17.0.11:win")
    implementation("org.openjfx:javafx-graphics:17.0.11:win")
    implementation("org.openjfx:javafx-controls:17.0.11:win")
    implementation("org.openjfx:javafx-fxml:17.0.11:win") // FXML 코드가 있으면 필요

    // 기존 의존성 (OpenJFX 끌려오지 않게 exclude 유지)
    implementation("org.controlsfx:controlsfx:11.1.2") { exclude(group = "org.openjfx") }
    implementation("com.dlsc.formsfx:formsfx-core:11.6.0") { exclude(group = "org.openjfx") }
    implementation("com.fazecast:jSerialComm:2.10.4")
    val fxyzVersion = "0.6.0"
    implementation("org.fxyz3d:fxyz3d:$fxyzVersion") { exclude(group = "org.openjfx") }
    implementation("org.fxyz3d:fxyz3d-importers:$fxyzVersion") { exclude(group = "org.openjfx") }

    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    // 스프링 부트 메인은 그대로 유지
    mainClass.set("com.example.BalanceStage.BalanceStageApplicationKt")
}

kotlin {
    compilerOptions { freeCompilerArgs.add("-Xjsr305=strict") }
}

tasks.withType<Test> {
    useJUnitPlatform()    // ✅ 괄호 닫기
}

/** ✅ JavaFX 뷰어 전용 실행 태스크 (module-path 지정) */
tasks.register<JavaExec>("runBalanceSurface") {
    group = "application"
    description = "Run the JavaFX RBF surface viewer"
    mainClass.set("plot.BalanceSurfaceApp")

    // 우리 앱 + 의존성 클래스패스
    classpath = sourceSets["main"].runtimeClasspath

    // runtimeClasspath에서 JavaFX JAR들만 골라 module-path 구성
    val fxModulePath = configurations.runtimeClasspath.get()
        .files
        .filter { it.name.startsWith("javafx-") }   // javafx-base/graphics/controls/fxml...
        .joinToString(separator = System.getProperty("path.separator")) { it.absolutePath }

    // 모듈 경로/모듈 지정
    jvmArgs = listOf(
        "--module-path", fxModulePath,
        "--add-modules", "javafx.controls,javafx.graphics,javafx.fxml"
    )
}





/*
plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
    application
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // ✅ JavaFX는 '컴파일 전용'으로만 추가 (런타임은 VM 옵션의 module-path가 담당)
    //    버전/플랫폼을 VM 옵션의 SDK(17.0.16 + win)와 정확히 맞춥니다.
    //compileOnly("org.openjfx:javafx-base:17.0.16:win")
    //compileOnly("org.openjfx:javafx-graphics:17.0.16:win")
    //compileOnly("org.openjfx:javafx-controls:17.0.16:win")
    //compileOnly("org.openjfx:javafx-fxml:17.0.16:win")
    // ※ swing/web/media가 필요하면 동일 방식으로 compileOnly 추가

    implementation("org.openjfx:javafx-base:17.0.11:win")
    implementation("org.openjfx:javafx-graphics:17.0.11:win")
    implementation("org.openjfx:javafx-controls:17.0.11:win")
    implementation("org.openjfx:javafx-fxml:17.0.11:win")

    // UI/폼 라이브러리 (OpenJFX를 끌어오지 않도록 차단)
    implementation("org.controlsfx:controlsfx:11.1.2") {
        exclude(group = "org.openjfx")
    }
    implementation("com.dlsc.formsfx:formsfx-core:11.6.0") {
        exclude(group = "org.openjfx")
    }

    // 시리얼 통신
    implementation("com.fazecast:jSerialComm:2.10.4")

    // FXyz 3D (OpenJFX 끌어오지 않게 차단)
    val fxyzVersion = "0.6.0"
    implementation("org.fxyz3d:fxyz3d:$fxyzVersion") {
        exclude(group = "org.openjfx")
    }
    implementation("org.fxyz3d:fxyz3d-importers:$fxyzVersion") {
        exclude(group = "org.openjfx")
    }

    // JSON
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    // Kotlin 메인 클래스는 보통 Kt가 붙습니다.
    mainClass.set("com.example.BalanceStage.BalanceStageApplicationKt")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
*/
