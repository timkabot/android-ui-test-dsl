pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "AndroidUiTestDsl"
include(":ui-test-dsl")
if (!providers.gradleProperty("sdkOnly").isPresent) include(":app")
