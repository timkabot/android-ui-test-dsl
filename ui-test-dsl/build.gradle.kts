plugins {
    id("org.jetbrains.kotlin.jvm")
    `java-library`
    `maven-publish`
}

group = "io.github.timkabot"
version = "0.1.0"
kotlin { jvmToolchain(17) }
java { withSourcesJar() }

dependencies {
    api("junit:junit:4.13.2")
    api("io.insert-koin:koin-core:4.2.1")
    api("com.squareup.okhttp3:mockwebserver:5.3.0")
    api("com.apollographql.apollo:apollo-runtime:4.4.3")
    api("com.apollographql.apollo:apollo-testing-support:4.4.3")
}

publishing {
    publications {
        create<MavenPublication>("sdk") {
            from(components["java"])
            pom {
                licenses { license { name.set("MIT"); url.set("https://opensource.org/license/mit/") } }
                name.set("Android UI Test DSL")
                description.set("JUnit4 test environments with Koin overrides, REST, Apollo and feature flags")
                url.set("https://github.com/timkabot/android-ui-test-dsl")
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
