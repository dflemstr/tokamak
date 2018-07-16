import com.diffplug.gradle.spotless.SpotlessExtension
import net.ltgt.gradle.apt.*

group = "io.dflemstr"
version = "0.1.0-SNAPSHOT"

plugins {
    java
    id("com.diffplug.gradle.spotless") version "3.13.0"
    id("net.ltgt.apt") version "0.17"
    id("net.ltgt.apt-idea") version "0.17"
}

repositories {
    mavenCentral()
}

dependencies {
    compile("com.google.guava", "guava", "25.1-jre")
    compile("org.slf4j", "slf4j-api", "1.7.25")
    compileOnly("com.google.auto.value", "auto-value-annotations", "1.6.2")
    annotationProcessor("com.google.auto.value", "auto-value", "1.6.2")
    testCompile("junit", "junit", "4.12")
    testCompile("org.hamcrest", "java-hamcrest", "2.0.0.0")
    testCompile("ch.qos.logback", "logback-classic", "1.2.3")
}

configure<SpotlessExtension> {
    java {
        googleJavaFormat()
        // you can then layer other format steps, such as
        licenseHeaderFile("license.java")
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
