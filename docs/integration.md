# Integrate the SDK

Use `io.github.timkabot.uitestdsl.*` in instrumentation tests. The SDK has no dependency on the sample Activity, its domain interfaces, Retrofit, or Compose. It provides resources; your adapter binds those resources to your app.

## REST

Create the server during environment evaluation, then bind your real client to its URL. The following example uses the sample app's contracts; substitute your own API and repository:

```kotlin
val environment = UiTestEnvironment(defaultModules = { listOf(demoModule()) }) {
    val server = restMockServer {
        POST("/promocodes") returns 200 withBodyContaining "EXPIRED" withJson
            """{"accepted":false,"message":"Срок действия промокода истёк"}"""
    }
    val api = Retrofit.Builder().baseUrl(server.url("/"))
        .build().create(PromoApi::class.java)
    dependencies {
        factory<AddPromocode> { RestAddPromocode(api) }
    }
}
```

Wrap your Activity rule with `environment.around(composeTestRule)` and expose the result as **one** `@get:Rule`. Do not also annotate the inner Compose rule: that would evaluate it twice. For repetition, move the binding code into your app's `BaseUiTest`, as the sample does.

Android needs `INTERNET` permission. For local HTTP testing, allow cleartext traffic in the **debug** manifest/network security configuration only. The SDK cannot redirect clients your app has already created, nor automatically replace every backend in your app. Read the recorded server requests when diagnosing a mismatch.

Routing matches method, exact path (including query string), and an optional body substring. First match wins; unmatched requests return 404. JSON matching is textual, not structural. `withJsonSequence` consumes prepared bodies then falls back to `withJson` (empty by default). Configure all routes before launching the Activity.

## Apollo GraphQL

In the environment block:

```kotlin
val client = mockGraphQl {
    registerTestResponse(
        ProductsQuery(),
        ProductsQuery.Data(listOf(ProductsQuery.Product("1", "Headphones"))),
    )
}
dependencies {
    factory<GetProducts> { GraphQlGetProducts(client) }
}
```

`registerTestResponse` is Apollo's extension from `com.apollographql.apollo.testing`; it may require `@OptIn(ApolloExperimental::class)` in your test. Operations and generated models belong to your app. The SDK creates `MapTestNetworkTransport` and closes the client after the inner rule. Typed responses test application handling of operation data, not HTTP, authentication or JSON decoding.

## Feature flags

```kotlin
val flags = mockFeatureFlags(mapOf("promo_enabled" to true)) {
    set("promo_enabled", false)
}
dependencies {
    single<AppFeatureFlags> { AppFeatureFlags(flags::isEnabled) }
}
```

`AppFeatureFlags` represents your app's interface; adapt the snapshot to it. Values are Boolean, unknown names return false, and snapshots do not change after construction. No production flag service is required by the SDK. Runtime updates and experiments are outside this version's scope.

## Koin lifecycle

`defaultModules` is evaluated for each test and must return **fresh modules containing every dependency the Activity requires**. The rule stops the existing global container, configures resources, then starts Koin with defaults followed by overrides. Ordinary `factory` and `single` declarations are available inside `dependencies`.

For an Android context, use the Koin Android extension in your test adapter:

```kotlin
UiTestEnvironment(
    defaultModules = { appModules() },
    configureKoin = {
        androidContext(InstrumentationRegistry.getInstrumentation().targetContext)
    },
) {
    dependencies { /* app bindings */ }
}.around(composeTestRule)
```

Import `org.koin.android.ext.koin.androidContext` and `androidx.test.platform.app.InstrumentationRegistry` from your test dependencies. `configureKoin` is for container settings; provide modules through `defaultModules` and `dependencies` so override ordering stays explicit.

The Android Application starts before JUnit rules. If it resolves dependencies or starts background jobs in `onCreate`, use a test Application/runner to prevent that eager startup. Starting the rule does not replace already-held references or cancel existing jobs. The rule stops Koin after the test; it does not restore the previous container. Avoid other tests that expect that old container to remain running. Never execute these rules concurrently in a single process.

## App-owned resources

Use `onClose { ... }` inside the environment block for app-specific resources. Cleanup attempts all actions in reverse order, after stopping Koin and after the inner rule has closed the Activity. A setup failure also triggers cleanup. Cleanup failures are suppressed onto an existing test failure; otherwise they fail the test themselves.

The SDK does not reset databases, preferences, accounts or background jobs. Put the reset required by your scenario in your adapter. In particular, state required at screen startup must be prepared **before** the Activity rule evaluates.
