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

// <- 여기는 보통 생략 가능하지만, 하위모듈/특수구성 아니면 있어도 무방
repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // ✅ ARM(M1/M2)용 JavaFX
    implementation("org.openjfx:javafx-base:17.0.2:mac-aarch64")
    implementation("org.openjfx:javafx-graphics:17.0.2:mac-aarch64")
    implementation("org.openjfx:javafx-controls:17.0.2:mac-aarch64")
    implementation("org.openjfx:javafx-fxml:17.0.2:mac-aarch64")

    implementation("org.controlsfx:controlsfx:11.1.2")
    implementation("com.dlsc.formsfx:formsfx-core:11.6.0") { exclude(group = "org.openjfx") }

    implementation("com.fazecast:jSerialComm:2.10.4")

    val fxyzVersion = "0.6.0"
    implementation("org.fxyz3d:fxyz3d:$fxyzVersion")
    implementation("org.fxyz3d:fxyz3d-importers:$fxyzVersion")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


application {
    mainClass.set("com.example.BalanceStage.BalanceStageApplication")
}


tasks.withType<Test> { useJUnitPlatform() }
