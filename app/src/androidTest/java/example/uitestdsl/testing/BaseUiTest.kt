package example.uitestdsl.testing

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.testing.MapTestNetworkTransport
import example.uitestdsl.*
import okhttp3.mockwebserver.MockWebServer
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import retrofit2.Retrofit

@UiTestDsl
class FeatureFlagsDsl {
    internal val values = mutableMapOf(Flags.PROMOCODE_ENABLED to true)
    fun set(name: String, enabled: Boolean) { values[name] = enabled }
}

@OptIn(ApolloExperimental::class)
abstract class BaseUiTest {
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    private val cleanup = mutableListOf<() -> Unit>()

    fun makeUiTestRule(configure: Module.() -> Unit = {}): TestRule {
        val environment = TestRule { base, _: Description ->
            object : Statement() {
                override fun evaluate() {
                    stopKoin()
                    var primaryFailure: Throwable? = null
                    try {
                        // Evaluate DSL inside the rule: servers must not start in test constructors.
                        val overrides = module { configure() }
                        startKoin { allowOverride(true); modules(demoModule(), overrides) }
                        base.evaluate()
                    } catch (failure: Throwable) {
                        primaryFailure = failure
                        throw failure
                    } finally {
                        // Compose rule has already closed the Activity at this point.
                        val actions = listOf<() -> Unit>({ stopKoin() }) + cleanup.asReversed().toList()
                        cleanup.clear()
                        val failures = actions.mapNotNull { runCatching(it).exceptionOrNull() }
                        if (failures.isNotEmpty()) {
                            val failure = primaryFailure ?: failures.first()
                            failures.filter { it !== failure }.forEach(failure::addSuppressed)
                            if (primaryFailure == null) throw failure
                        }
                    }
                }
            }
        }
        return RuleChain.outerRule(environment).around(composeTestRule)
    }

    fun Module.restMockServer(block: RestMockDsl.() -> Unit) {
        val server = MockWebServer()
        server.dispatcher = RestMockDsl().apply(block).dispatcher()
        server.start()
        cleanup += { server.shutdown() }
        val api = Retrofit.Builder().baseUrl(server.url("/"))
            .build().create(PromoApi::class.java)
        factory<AddPromocode> { RestAddPromocode(api) }
    }

    fun Module.mockGraphQl(block: ApolloClient.() -> Unit) {
        val client = ApolloClient.Builder().networkTransport(MapTestNetworkTransport()).build()
        cleanup += { client.close() }
        client.block()
        factory<GetProducts> { GraphQlGetProducts(client) }
    }

    fun Module.mockFeatureFlags(block: FeatureFlagsDsl.() -> Unit) {
        val values = FeatureFlagsDsl().apply(block).values.toMap()
        single<FeatureFlags> { FeatureFlags { values[it] ?: false } }
    }
}
