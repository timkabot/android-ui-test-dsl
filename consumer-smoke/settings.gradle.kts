pluginManagement { repositories { mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositories {
        val bundle = providers.gradleProperty("sdkRepository").orNull
        if (bundle != null) {
            maven { url = uri(bundle); content { includeGroup("io.github.timkabot") } }
        } else {
            mavenLocal { content { includeGroup("io.github.timkabot") } }
        }
        mavenCentral()
    }
}
rootProject.name = "sdk-consumer-smoke"
