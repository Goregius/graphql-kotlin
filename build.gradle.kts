
import io.gitlab.arturbosch.detekt.detekt
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI
import java.time.Instant

description = "Libraries for running a GraphQL server in Kotlin"
extra["isReleaseVersion"] = !version.toString().endsWith("SNAPSHOT")

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka") apply false
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
    jacoco
    signing
    `maven-publish`
}

allprojects {
    buildscript {
        repositories {
            mavenLocal()
            jcenter()
            mavenCentral()
        }
    }

    repositories {
        mavenLocal()
        jcenter()
        mavenCentral()
    }
}

subprojects {
    val kotlinVersion: String by project
    val junitVersion: String by project
    val mockkVersion: String by project

    val detektVersion: String by project
    val ktlintVersion: String by project
    val jacocoVersion: String by project

    val currentProject = this

    apply(plugin = "kotlin")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "jacoco")
    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    tasks {
        detekt {
            toolVersion = detektVersion
            config = files("${rootProject.projectDir}/detekt.yml")
        }
        ktlint {
            version.set(ktlintVersion)
        }
        jacoco {
            toolVersion = jacocoVersion
        }
        build {
            dependsOn(jacocoTestReport)
        }
        jar {
            manifest {
                attributes["Built-By"] = "Expedia Group"
                attributes["Build-Jdk"] = "${System.getProperty("java.version")} (${System.getProperty("java.vendor")} ${System.getProperty("java.vm.version")})"
                attributes["Build-Timestamp"] = Instant.now().toString()
                attributes["Created-By"] = "Gradle ${gradle.gradleVersion}"
                attributes["Implementation-Title"] = currentProject.name
                attributes["Implementation-Version"] = project.version
            }
        }
        test {
            useJUnitPlatform()
        }

        // published artifacts
        val jarComponent = currentProject.components.getByName("java")
        val sourcesJar by registering(Jar::class) {
            archiveClassifier.set("sources")
            from(sourceSets.main.get().allSource)
        }
        val dokka by getting(DokkaTask::class) {
            outputFormat = "javadoc"
            outputDirectory = "$buildDir/javadoc"
        }
        val javadocJar by registering(Jar::class) {
            archiveClassifier.set("javadoc")
            from("$buildDir/javadoc")
            dependsOn(dokka.path)
        }
        publishing {
            repositories {
                maven {
                    name = "ossrh"
                    url = URI.create("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                    credentials {
                        username = System.getenv("SONATYPE_USERNAME")
                        password = System.getenv("SONATYPE_PASSWORD")
                    }
                }
            }
            publications {
                create<MavenPublication>("mavenJava") {
                    pom {
                        url.set("https://github.com/ExpediaGroup/graphql-kotlin")
                        licenses {
                            license {
                                name.set("The Apache Software License, Version 2.0")
                                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                            }
                        }
                        organization {
                            name.set("Expedia Group")
                            name.set("https://www.expediagroup.com/")
                        }
                        developers {
                            developer {
                                name.set("Expedia Group Committers")
                                email.set("oss@expediagroup.com")
                                organization.set("Expedia Group")
                                organizationUrl.set("https://www.expediagroup.com/")
                            }
                        }
                        scm {
                            connection.set("scm:git:git://github.com/ExpediaGroup/graphql-kotlin.git")
                            developerConnection.set("scm:git:git://github.com/ExpediaGroup/graphql-kotlin.git")
                            url.set("https://github.com/ExpediaGroup/graphql-kotlin")
                        }
                    }
                    from(jarComponent)
                    artifact(sourcesJar.get())
                    artifact(javadocJar.get())
                }
            }
        }
        signing {
            setRequired {
                (rootProject.extra["isReleaseVersion"] as Boolean) && gradle.taskGraph.hasTask("publish")
            }
            val signingKey: String? = System.getenv("GPG_SECRET")
            val signingPassword: String? = System.getenv("GPG_PASSPHRASE")
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(publishing.publications["mavenJava"])
        }
    }

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
        testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
        testImplementation("io.mockk:mockk:$mockkVersion")
    }
}

tasks {
    jar {
        enabled = false
    }
}
