dependencies {
    implementation(project(":shared-kernel"))

    implementation(rootProject.libs.spring.boot.starter)
    implementation(rootProject.libs.spring.boot.starter.web)
    implementation(rootProject.libs.spring.boot.starter.data.jpa)
    implementation(rootProject.libs.spring.boot.starter.validation)
    implementation(rootProject.libs.spring.boot.starter.data.redis)
    implementation(rootProject.libs.spring.boot.starter.data.elasticsearch)
    implementation(rootProject.libs.spring.boot.starter.oauth2.resource.server)

    implementation(rootProject.libs.spring.modulith.starter.core)
    implementation(rootProject.libs.spring.modulith.starter.jpa)

    implementation(rootProject.libs.minio)

    testImplementation(rootProject.libs.spring.modulith.starter.test)
    testImplementation(rootProject.libs.spring.boot.data.jpa.test)
    testImplementation(rootProject.libs.spring.boot.jdbc.test)
    testImplementation(rootProject.libs.spring.boot.webmvc.test)
    testImplementation(rootProject.libs.spring.security.test)
    testImplementation(rootProject.libs.spring.boot.data.redis.test)
    testImplementation(rootProject.libs.spring.boot.data.elasticsearch.test)
    testImplementation(rootProject.libs.testcontainers.postgresql)
    testImplementation(rootProject.libs.testcontainers.elasticsearch)
    testImplementation(rootProject.libs.awaitility)

    testRuntimeOnly(rootProject.libs.postgresql)
    testRuntimeOnly(rootProject.libs.flyway.core)
    testRuntimeOnly(rootProject.libs.flyway.postgresql)
}
