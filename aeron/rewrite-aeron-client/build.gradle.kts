plugins {
    id("groovy")
}

group = "com.yuanzhixiang"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val mockitoVersion = "4.9.0"
val junitVersion = "5.9.1"

dependencies {
    implementation(project(":agrona:rewrite-agrona"))
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${junitVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}