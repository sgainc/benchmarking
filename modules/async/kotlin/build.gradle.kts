plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "nw2s"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-quartz")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("io.lettuce:lettuce-core:6.7.1.RELEASE")

	implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
	implementation("ch.qos.logback.contrib:logback-jackson:0.1.5")
	implementation("ch.qos.logback.contrib:logback-json-classic:0.1.5")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

	implementation("software.amazon.awssdk:s3:2.31.59")
	implementation("software.amazon.awssdk:netty-nio-client:2.31.62")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

springBoot {
	mainClass.set("application.Application")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
