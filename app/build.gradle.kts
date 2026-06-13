plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":shared-kernel"))
    implementation(project(":product"))
    implementation(project(":payment"))
    implementation(project(":inventory"))
    implementation(project(":infrastructure"))

    implementation(rootProject.libs.spring.boot.starter.web)
    implementation(rootProject.libs.spring.boot.starter.actuator)
    implementation(rootProject.libs.spring.boot.starter.oauth2.resource.server)

    implementation(rootProject.libs.spring.modulith.starter.core)
    implementation(rootProject.libs.spring.modulith.starter.jpa)

    runtimeOnly(rootProject.libs.spring.modulith.docs)
    runtimeOnly(rootProject.libs.postgresql)
    runtimeOnly(rootProject.libs.flyway.core)
    runtimeOnly(rootProject.libs.flyway.postgresql)

    testImplementation(rootProject.libs.spring.modulith.starter.test)
    testImplementation(rootProject.libs.spring.boot.resttestclient)
    testImplementation(rootProject.libs.testcontainers.postgresql)
    testImplementation(rootProject.libs.testcontainers.elasticsearch)
    testImplementation(rootProject.libs.testcontainers.vault)
    testImplementation(rootProject.libs.testcontainers.keycloak)
    testImplementation(rootProject.libs.archunit.junit5)
    testImplementation(rootProject.libs.awaitility)
    testImplementation(rootProject.libs.cucumber.java)
    testImplementation(rootProject.libs.cucumber.spring)
    testImplementation(rootProject.libs.cucumber.junit.platform)
    testImplementation("org.junit.platform:junit-platform-suite")
    testImplementation(rootProject.libs.pact.provider.junit5)
}

/**
 * 把 Modulith Documenter 產出的 PlantUML + AsciiDoc 從 build/ 搬到 docs/spring-modulith/,
 * 讓人類讀得到、git 也追得到。跑 ./gradlew syncModulithDocs 觸發。
 */
val syncModulithDocs by tasks.registering(Copy::class) {
    description = "Copy generated Modulith docs (PlantUML / AsciiDoc) to docs/spring-modulith/."
    group = "documentation"
    dependsOn(tasks.named("test"))
    from(layout.buildDirectory.dir("spring-modulith-docs"))
    into(rootProject.layout.projectDirectory.dir("docs/spring-modulith"))
}

