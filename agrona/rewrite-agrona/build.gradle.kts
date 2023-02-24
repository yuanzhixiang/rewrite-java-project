plugins {
    id("groovy")
}

group = "com.yuanzhixiang"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val byteBuddyVersion = "1.12.20"
val findbugsAnnotationsVersion = "3.0.1"
val checkstyleVersion = "9.3"
val hamcrestVersion = "2.2"
val mockitoVersion = "4.10.0"
val junitVersion = "5.9.1"
val guavaTestLib = "31.1-jre"
val junit4Version = "4.13.2"
val jmhVersion = "1.36"
val jcstressVersion = "0.15"

dependencies {
//    implementation("org.apache.groovy:groovy:4.0.2")
//    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
//    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

//    testImplementation files ('build/classes/java/generated')
    testImplementation("org.hamcrest:hamcrest:${hamcrestVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${junitVersion}")
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
    testImplementation("com.google.guava:guava-testlib:${guavaTestLib}")
    testImplementation("junit:junit:${junit4Version}") // Compatibility with JUnit 4
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:${junitVersion}")
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