plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openjfx.javafxplugin") version "0.0.13"
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

javafx {
    version = "17.0.2"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics", "javafx.base")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.controlsfx:controlsfx:11.1.2")
    implementation("com.dlsc.formsfx:formsfx-core:11.6.0") { exclude(group = "org.openjfx") }

    implementation("com.fazecast:jSerialComm:2.10.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("org.apache.commons:commons-math3:3.6.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


application {
    mainClass.set("com.example.BalanceStage.BalanceStageApplication")
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs = listOf(
        "--module-path", classpath.asPath,
        "--add-modules", "javafx.controls,javafx.fxml,javafx.graphics,javafx.base"
    )
}

tasks.withType<Test> { useJUnitPlatform() }
