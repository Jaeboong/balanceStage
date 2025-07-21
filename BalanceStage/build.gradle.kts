plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.3"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.openjfx.javafxplugin") version "0.1.0"
	application
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(17))
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// JavaFX modules - 모든 필요한 모듈 추가
	implementation("org.openjfx:javafx-controls:21")
	implementation("org.openjfx:javafx-fxml:21")
	implementation("org.openjfx:javafx-graphics:21")
	implementation("org.openjfx:javafx-base:21")

	// JavaFX UI libraries
	implementation("org.controlsfx:controlsfx:11.1.2")
	implementation("com.dlsc.formsfx:formsfx-core:11.6.0") {
		exclude(group = "org.openjfx")
	}

	// Serial communication
	implementation("com.fazecast:jSerialComm:2.10.4")

	// FXyz 3D & GLB/GLTF Importer
	val fxyzVersion = "0.6.0"
	implementation("org.fxyz3d:fxyz3d:$fxyzVersion")
	implementation("org.fxyz3d:fxyz3d-importers:$fxyzVersion")

	// JSON for 3D models
	implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
	mainClass.set("com.example.BalanceStage.BalanceStageApplicationKt")
}

javafx {
	version = "21"
	modules("javafx.controls", "javafx.fxml", "javafx.graphics", "javafx.base")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// JavaFX 애플리케이션 실행을 위한 설정
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
	jvmArgs = listOf(
		"--add-opens", "javafx.graphics/com.sun.javafx.iio=ALL-UNNAMED",
		"--add-opens", "javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED",
		"--add-opens", "javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED"
	)
}

// 일반 Java 애플리케이션 실행을 위한 설정
tasks.named<JavaExec>("run") {
	jvmArgs = listOf(
		"--add-opens", "javafx.graphics/com.sun.javafx.iio=ALL-UNNAMED",
		"--add-opens", "javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED",
		"--add-opens", "javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED"
	)
}