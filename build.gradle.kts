plugins {
    id("groovy")
}

group = "com.yuanzhixiang"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

fun getBuildJavaVersion0(): Int {
    var buildJavaVersion = System.getenv("BUILD_JAVA_VERSION") ?: JavaVersion.current().getMajorVersion()
    if (buildJavaVersion.indexOf('.') > 0) {
        buildJavaVersion = buildJavaVersion.substring(0, buildJavaVersion.indexOf('.'))
    }
    if (buildJavaVersion.indexOf('-') > 0) {
        buildJavaVersion = buildJavaVersion.substring(0, buildJavaVersion.indexOf('-'))
    }
    return Integer.parseInt(buildJavaVersion)
}

val buildJavaVersion = getBuildJavaVersion0()

tasks.getByName<Test>("test") {
    if (buildJavaVersion >= 9) {
        jvmArgs("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
    }
    useJUnitPlatform()
}