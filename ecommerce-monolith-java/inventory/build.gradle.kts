dependencies {
    implementation(project(":shared-kernel"))

    implementation(rootProject.libs.spring.boot.starter)
    implementation(rootProject.libs.spring.boot.starter.data.jpa)
    implementation(rootProject.libs.spring.boot.starter.data.redis)

    implementation(rootProject.libs.spring.modulith.starter.core)
    implementation(rootProject.libs.spring.modulith.starter.jpa)

    implementation(rootProject.libs.vault)

    testImplementation(rootProject.libs.spring.modulith.starter.test)
    testImplementation(rootProject.libs.testcontainers.postgresql)
    testImplementation(rootProject.libs.testcontainers.vault)
    testImplementation(rootProject.libs.awaitility)
}
