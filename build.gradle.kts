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
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	
	// JavaFX dependencies
	implementation("org.openjfx:javafx-controls:21")
	implementation("org.openjfx:javafx-fxml:21")
	implementation("org.controlsfx:controlsfx:11.1.2")
	implementation("com.dlsc.formsfx:formsfx-core:11.6.0") {
		exclude(group = "org.openjfx")
	}
	
	// Serial Communication
	implementation("com.fazecast:jSerialComm:2.10.4")
	
	// FXyz 3D + GLB/GLTF Importers (0.6.x 정식 아티팩트 이름)
	val fxyzVersion = "0.6.0"

	implementation("org.fxyz3d:fxyz3d:$fxyzVersion")
	implementation("org.fxyz3d:fxyz3d-importers:$fxyzVersion")

	
	// JSON Processing for 3D Models (FXyz3D에서도 사용)
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
    modules("javafx.controls", "javafx.fxml")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs = listOf(
        "--module-path", configurations.runtimeClasspath.get().asPath,
        "--add-modules", "javafx.controls,javafx.fxml,javafx.graphics",
        "--add-opens", "javafx.graphics/com.sun.javafx.iio=ALL-UNNAMED"
    )
}
