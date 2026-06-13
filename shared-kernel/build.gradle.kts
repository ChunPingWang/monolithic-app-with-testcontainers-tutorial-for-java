dependencies {
    implementation(rootProject.libs.spring.modulith.api)
    implementation(rootProject.libs.spring.modulith.events.api)

    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}
