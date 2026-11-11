plugins {
	java
	id("org.springframework.boot") version "4.1.2"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.diffplug.spotless") version "8.11.0"
}

group = "io.github.sushantn16"
version = "0.0.1-SNAPSHOT"

// Compile for Java 21 with whichever JDK runs Gradle, as long as it's 21 or newer. A toolchain
// would require exactly JDK 21 to be installed.
tasks.withType<JavaCompile> {
	options.release = 21
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// ./gradlew spotlessApply formats the code; ./gradlew check (and build) fails if it isn't formatted.
spotless {
	java {
		palantirJavaFormat("2.103.0")
		removeUnusedImports()
	}
}
