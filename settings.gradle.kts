pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { mavenCentral(); google() }
}
rootProject.name = "AndroidUiTestDsl"
include(":ui-test-dsl")
if (!providers.gradleProperty("sdkOnly").isPresent) include(":app")
