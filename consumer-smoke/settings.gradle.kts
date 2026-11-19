pluginManagement { repositories { mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositories {
        val bundle = providers.gradleProperty("sdkRepository").orNull
        if (bundle != null) {
            maven { url = uri(bundle); content { includeGroup("io.github.timkabot") } }
        } else if (!providers.gradleProperty("centralOnly").isPresent) {
            mavenLocal { content { includeGroup("io.github.timkabot") } }
        }
        mavenCentral()
    }
}
rootProject.name = "sdk-consumer-smoke"
