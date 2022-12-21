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
    implementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    implementation("org.mockito:mockito-core:${mockitoVersion}")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}