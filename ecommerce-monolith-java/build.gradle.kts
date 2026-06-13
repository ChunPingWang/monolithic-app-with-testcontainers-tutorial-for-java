plugins {
    java
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.spring.boot) apply false
}

allprojects {
    group = "com.tutorial.ecommerce"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.projectlombok") {
                useVersion(rootProject.libs.versions.lombok.get())
            }
        }
    }

    repositories {
        mavenCentral()
    }

    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:${rootProject.libs.versions.springBoot.get()}")
            mavenBom("org.springframework.modulith:spring-modulith-bom:${rootProject.libs.versions.springModulith.get()}")
            mavenBom("org.testcontainers:testcontainers-bom:${rootProject.libs.versions.testcontainers.get()}")
        }
    }

    dependencies {
        "compileOnly"(rootProject.libs.lombok)
        "annotationProcessor"(rootProject.libs.lombok)
        "testCompileOnly"(rootProject.libs.lombok)
        "testAnnotationProcessor"(rootProject.libs.lombok)

        "testImplementation"(rootProject.libs.spring.boot.starter.test)
        "testImplementation"(rootProject.libs.testcontainers.junit)
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(listOf(
            "-parameters",
            "-Xlint:all",
            "-Xlint:-processing",
            "-Xlint:-serial",
            "-Xlint:-classfile",
            "-Werror"
        ))
        options.encoding = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        systemProperty("file.encoding", "UTF-8")
    }

    tasks.named<Test>("test") {
        // 預設 test 只跑 *Test(快速、無容器)。容器整合測試走 integrationTest,Cucumber BDD 走 bddTest。
        exclude("**/*IT.class")
        exclude("**/bdd/**")
    }

    val integrationTest = tasks.register<Test>("integrationTest") {
        description = "Run Testcontainers integration tests (require Docker)."
        group = "verification"
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].runtimeClasspath
        include("**/*IT.class")
        useJUnitPlatform()
        shouldRunAfter("test")
        systemProperty("file.encoding", "UTF-8")
    }
    tasks.named("check") { dependsOn(integrationTest) }
}
