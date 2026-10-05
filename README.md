# Android UI Test DSL

A Kotlin/JUnit4 library for preparing UI test environments: Koin overrides, local REST responses, typed Apollo responses and Boolean feature flags. An Android Compose sample shows how to connect it to an app.

```kotlin
@get:Rule
val rule = makeUiTestRule {
    factory<AddPromocode> {
        AddPromocode { PromoResult.Rejected("Срок действия промокода истёк") }
    }
}

@Test
fun expiredPromoShowsError() {
    PromoScreen(composeTestRule).apply {
        submit("EXPIRED")
        assertResult("Срок действия промокода истёк")
    }
}
```

This scenario uses the sample's [BaseUiTest adapter](app/src/androidTest/java/example/uitestdsl/testing/BaseUiTest.kt) and [Page Object](app/src/androidTest/java/example/uitestdsl/testing/PromoScreen.kt). The SDK manages resource lifetime; the adapter connects the SDK to your Activity and app interfaces.

## Add it to your project

Version **0.1.0**, [MIT license](LICENSE). Requires JDK 17 and Kotlin 2.3.21 or a compatible compiler. Tested with Koin 4.2.1, OkHttp 5.3.0 and Apollo Kotlin 4.4.3. This initial SDK includes these integrations in one artifact.

Download `ui-test-dsl-0.1.0-maven.zip` from [Releases](https://github.com/timkabot/android-ui-test-dsl/releases/tag/v0.1.0) and extract it into `vendor/ui-test-dsl` in your project. The extracted directory must directly contain `io/`.

In your `settings.gradle.kts`, add the extracted Maven repository alongside your existing repositories:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("vendor/ui-test-dsl")
            content { includeGroup("io.github.timkabot") }
        }
        google()
        mavenCentral()
    }
}
```

In your app's `build.gradle.kts`:

```kotlin
dependencies {
    androidTestImplementation("io.github.timkabot:ui-test-dsl:0.1.0")
}
```

The ZIP contains the library, sources and Maven dependency metadata. Gradle downloads transitive dependencies from Maven Central. Add it to `androidTestImplementation`, not your production dependencies. This version is distributed through GitHub Releases; it is not published to Maven Central or JitPack.

Alternatively, build from source and install into your local Maven cache (no Android SDK required for the library):

```sh
git clone --branch v0.1.0 https://github.com/timkabot/android-ui-test-dsl.git
cd android-ui-test-dsl
./gradlew -PsdkOnly :ui-test-dsl:publishToMavenLocal
```

Then use `mavenLocal { content { includeGroup("io.github.timkabot") } }` instead of the `vendor` repository. Other developers and CI also need to install the artifact or use the ZIP.

## Connect it to an Activity

Create an adapter under your app's `src/androidTest`. Replace `MainActivity` and `appModules()` with your Activity and a function returning **all** required Koin modules:

```kotlin
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import io.github.timkabot.uitestdsl.UiTestEnvironment
import org.junit.rules.TestRule
import org.koin.core.module.Module

abstract class BaseUiTest {
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    fun makeUiTestRule(configure: Module.() -> Unit): TestRule =
        UiTestEnvironment(defaultModules = { appModules() }) {
            dependencies(configure)
        }.around(composeTestRule)
}
```

Your project must already have its Android instrumentation runner and Compose test dependencies configured. `UiTestEnvironment` is an ordinary JUnit4 rule, so you can also wrap an `ActivityScenarioRule` for View-based tests.

The outer environment rule configures Koin before the inner rule launches the Activity, then closes resources after the Activity finishes. It owns and replaces the **process-global Koin container**. Do not run these rules concurrently in one process. It does not restore the application's old container after a test.

If your modules need Android context, pass `configureKoin = { androidContext(context) }` using your instrumentation target context and the Koin Android extension. `Application.onCreate` runs before test rules; dependencies resolved there cannot be replaced retroactively. Use a custom test Application/runner if startup must be controlled. [Integration guide](docs/integration.md) covers REST, Apollo and flags.

## Run the sample

Requires Android SDK Platform 36 and an emulator/device with API 26 or later. Set `ANDROID_HOME` or an untracked `local.properties`.

```sh
./gradlew :ui-test-dsl:test :app:assembleDebug :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

The sample's four tests cover Koin, REST, GraphQL and a disabled feature flag. Normal app use has offline defaults: enter `EXPIRED` for a rejection, any other nonblank code for success. UI text is in Russian.

## Source map

| Location | Purpose |
| --- | --- |
| [ui-test-dsl](ui-test-dsl/src/main/kotlin/io/github/timkabot/uitestdsl) | Reusable JUnit4 rule, request DSL and flag values |
| [BaseUiTest](app/src/androidTest/java/example/uitestdsl/testing/BaseUiTest.kt) | App-specific adapter using the SDK |
| [PromoTests](app/src/androidTest/java/example/uitestdsl/PromoTests.kt) | Complete scenarios and feature-level wrapper |
| [consumer-smoke](consumer-smoke) | Separate build consuming the published Maven artifact |
| [Integration guide](docs/integration.md) | Setup recipes and lifecycle constraints |
| [Implementation notes](docs/implementation.md) | Matching rules, cleanup and test coverage boundaries |

## Validate the published artifact

```sh
./gradlew -PsdkOnly :ui-test-dsl:test :ui-test-dsl:publishToMavenLocal :ui-test-dsl:distributionZip
./gradlew -p consumer-smoke test
```

The consumer build resolves the published artifact by Maven coordinates, without a project dependency on the sample or SDK source. Release bundles are written to `ui-test-dsl/build/distributions/`.

Verified on October 5, 2026: 9 SDK JVM tests, all 4 sample UI tests on Android 15 / API 35 arm64, and the independent consumer test using both Maven Local and the extracted release ZIP. Both APKs build; Android Lint reports no errors. This is a single-device check, not a device compatibility matrix.
