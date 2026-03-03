@file:Suppress("UnstableApiUsage")

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64

plugins {
    `java-library`
    `maven-publish`
    signing
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "org.jfxcore"
version = project.findProperty("TAG_VERSION_PROJECT") ?: "1.0-SNAPSHOT"

java {
    withSourcesJar()
    withJavadocJar()
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

javafx {
    version = project.ext["javafx.version"] as String
    modules = listOf("javafx.base", "javafx.graphics", "javafx.controls")
    configurations = arrayOf("compileOnly", "testImplementation")
}

dependencies {
    api("org.openjfx:javafx-base:${project.ext["javafx.version"]}")
    api("org.openjfx:javafx-graphics:${project.ext["javafx.version"]}")

    testImplementation("org.testfx:testfx-core:4.0.17")
    testImplementation("org.testfx:testfx-junit5:4.0.17")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        tags("implSpec:a:Implementation Requirements:")
    }
}

if (!version.toString().endsWith("-SNAPSHOT")) {
    val mavenCentralFixup by tasks.registering {
        doLast {
            val url = project.property("REPOSITORY_POST_URL") as String
            val username = project.property("REPOSITORY_USERNAME") as String
            val password = project.property("REPOSITORY_PASSWORD") as String
            val userToken = Base64.getEncoder().encodeToString("$username:$password".toByteArray(Charsets.UTF_8))
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer $userToken")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build()

            logger.info("POST $url")

            HttpClient.newHttpClient().use {
                val response = it.send(request, HttpResponse.BodyHandlers.ofString())
                logger.info("Received status code: ${response.statusCode()}")
            }
        }
    }

    tasks.publish {
        finalizedBy(mavenCentralFixup)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks.jar)
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
            pom {
                url.set("https://github.com/jfxcore/interactivity")
                name.set("interactivity")
                description.set("Reusable interactivity tools for JavaFX")
                licenses {
                    license {
                        name.set("GPL-2.0-only WITH Classpath-exception-2.0")
                        url.set("https://www.gnu.org/licenses/gpl-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("jfxcore")
                        name.set("JFXcore")
                        organization.set("JFXcore")
                        organizationUrl.set("https://github.com/jfxcore")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/jfxcore/interactivity.git")
                    developerConnection.set("scm:git:https://github.com/jfxcore/interactivity.git")
                    url.set("https://github.com/jfxcore/interactivity")
                }
            }
        }
    }
    repositories {
        maven {
            if (project.hasProperty("REPOSITORY_USERNAME")
                && project.hasProperty("REPOSITORY_PASSWORD")
                && project.hasProperty("REPOSITORY_URL")) {
                credentials {
                    username = project.property("REPOSITORY_USERNAME") as String
                    password = project.property("REPOSITORY_PASSWORD") as String
                }
                url = uri(project.property("REPOSITORY_URL") as String)
            }
        }
    }
}

signing {
    sign(publishing.publications["maven"])
}
