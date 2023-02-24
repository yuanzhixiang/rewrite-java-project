plugins {
    id("java")
}

group = "com.yuanzhixiang"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val mockitoVersion = "4.9.0"

dependencies {
    testImplementation("org.mockito:mockito-inline:${mockitoVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}