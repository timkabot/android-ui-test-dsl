plugins {
    id("org.jetbrains.kotlin.jvm")
    `java-library`
    `maven-publish`
    signing
    id("org.jetbrains.dokka") version "2.2.0"
}

group = "io.github.timkabot"
version = "0.1.1"
kotlin { jvmToolchain(17) }
java { withSourcesJar() }

dependencies {
    api("junit:junit:4.13.2")
    api("io.insert-koin:koin-core:4.2.1")
    api("com.squareup.okhttp3:mockwebserver:5.3.0")
    api("com.apollographql.apollo:apollo-runtime:4.4.3")
    api("com.apollographql.apollo:apollo-testing-support:4.4.3")
}

val apiDocsJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.named("dokkaGeneratePublicationHtml"))
}

publishing {
    publications {
        create<MavenPublication>("sdk") {
            from(components["java"])
            artifact(apiDocsJar)
            pom {
                licenses { license { name.set("MIT"); url.set("https://opensource.org/license/mit/") } }
                name.set("Android UI Test DSL")
                description.set("JUnit4 test environments with Koin overrides, REST, Apollo and feature flags")
                url.set("https://github.com/timkabot/android-ui-test-dsl")
                developers {
                    developer {
                        id.set("timkabot")
                        name.set("Timur Borgalinov")
                        url.set("https://github.com/timkabot")
                    }
                }
                scm {
                    url.set("https://github.com/timkabot/android-ui-test-dsl")
                    connection.set("scm:git:https://github.com/timkabot/android-ui-test-dsl.git")
                    developerConnection.set("scm:git:ssh://git@github.com/timkabot/android-ui-test-dsl.git")
                }
            }
        }
    }
    repositories { maven { name = "bundle"; url = layout.buildDirectory.dir("repository").get().asFile.toURI() } }
}

tasks.register<Zip>("distributionZip") {
    dependsOn("publishSdkPublicationToBundleRepository")
    from(layout.buildDirectory.dir("repository"))
    archiveFileName.set("ui-test-dsl-${project.version}-maven.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}

tasks.withType<Jar>().configureEach { from(rootProject.file("LICENSE")) { into("META-INF") } }

// Opt in only for release builds; normal tests do not require a signing key.
if (providers.gradleProperty("signRelease").isPresent) {
    signing {
        useGpgCmd()
        sign(publishing.publications["sdk"])
    }
}

tasks.register<Zip>("centralBundle") {
    dependsOn("publishSdkPublicationToBundleRepository")
    from(layout.buildDirectory.dir("repository")) {
        include("io/github/timkabot/ui-test-dsl/${project.version}/**")
    }
    archiveFileName.set("ui-test-dsl-${project.version}-central.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    doFirst {
        check(providers.gradleProperty("signRelease").isPresent) {
            "Central bundles must be signed: use -PsignRelease and configure your GPG key."
        }
    }
}
