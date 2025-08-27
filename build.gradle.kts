plugins {
	java
	id("org.springframework.boot") version "3.4.1"
	id("io.spring.dependency-management") version "1.1.7"
}

fun getGitHash(): String {
	return providers.exec {
		commandLine("git", "rev-parse", "--short", "HEAD")
	}.standardOutput.asText.get().trim()
}

group = "kr.hhplus.be"
version = getGitHash()

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.0")
	}
}

dependencies {
    // Spring
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework:spring-aspects")
	implementation("org.springframework.boot:spring-boot-starter-aop")

    // DB
	runtimeOnly("com.mysql:mysql-connector-j")

	//Swagger
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.0.4")

	// Lombok
	compileOnly("org.projectlombok:lombok:1.18.30")
	annotationProcessor("org.projectlombok:lombok:1.18.30")
	testCompileOnly("org.projectlombok:lombok:1.18.30")
	testAnnotationProcessor("org.projectlombok:lombok:1.18.30")

	// Redisson
	implementation("org.redisson:redisson-spring-boot-starter:3.23.4")

	// Redis Cache
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-cache")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:mysql")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

tasks.withType<Test> {
	useJUnitPlatform()

	// JVM 옵션 설정
	jvmArgs = listOf(
		"-Duser.timezone=UTC",
		"--add-opens", "java.base/java.lang=ALL-UNNAMED",
		"--add-opens", "java.base/java.util=ALL-UNNAMED"
	)

	// 테스트 메모리 설정
	minHeapSize = "512m"
	maxHeapSize = "2g"

	// 테스트 타임아웃 설정
	systemProperty("junit.jupiter.execution.timeout.default", "5m")

	// 병렬 테스트 설정 (동시성 테스트를 위해)
	systemProperty("junit.jupiter.execution.parallel.enabled", "true")
	systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")

	// 테스트 결과 출력
	testLogging {
		events("passed", "skipped", "failed")
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
		showExceptions = true
		showCauses = true
		showStackTraces = true
	}
}

// Gradle Wrapper 설정
tasks.wrapper {
	gradleVersion = "8.5"  // 안정적인 버전으로 고정
	distributionType = Wrapper.DistributionType.BIN
}