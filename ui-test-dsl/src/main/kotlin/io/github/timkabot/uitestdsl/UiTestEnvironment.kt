package io.github.timkabot.uitestdsl

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.testing.MapTestNetworkTransport
import okhttp3.mockwebserver.MockWebServer
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Owns the process-global Koin container for a test. Never run these rules concurrently
 * in one process. Supply ALL required application modules and a fresh module list per test.
 * Application.onCreate runs before instrumentation rules: startup-only dependencies need
 * a custom test Application. Existing resolved objects are not replaced by this rule.
 */
class UiTestEnvironment(
    private val defaultModules: () -> List<Module>,
    private val configureKoin: KoinApplication.() -> Unit = {},
    private val configure: UiTestEnvironment.() -> Unit = {},
) : TestRule {
    private val overrides = mutableListOf<Module>()
    private val cleanup = mutableListOf<() -> Unit>()
    private var active = false

    /** Configure overrides during rule evaluation, before the inner Activity rule. */
    fun dependencies(configure: Module.() -> Unit) {
        checkActive()
        overrides += module { configure() }
    }

    /** Register app-specific cleanup (preferences, stores, clients), run in reverse order. */
    fun onClose(action: () -> Unit) {
        checkActive()
        cleanup += action
    }

    fun restMockServer(configure: RestMockDsl.() -> Unit): MockWebServer {
        checkActive()
        val server = MockWebServer()
        // Register before starting so partially initialized resources are also released.
        onClose { server.shutdown() }
        server.dispatcher = RestMockDsl().apply(configure).dispatcher()
        server.start()
        return server
    }

    @OptIn(ApolloExperimental::class)
    fun mockGraphQl(configure: ApolloClient.() -> Unit): ApolloClient {
        checkActive()
        val client = ApolloClient.Builder().networkTransport(MapTestNetworkTransport()).build()
        onClose { client.close() }
        client.configure()
        return client
    }

    /** The inner rule must launch AND close the Activity inside its evaluate call. */
    fun around(inner: TestRule): TestRule = RuleChain.outerRule(this).around(inner)

    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            check(!active) { "UiTestEnvironment is already running" }
            active = true
            var primaryFailure: Throwable? = null
            try {
                stopKoin()
                this@UiTestEnvironment.configure()
                startKoin {
                    configureKoin()
                    allowOverride(true)
                    modules(defaultModules() + overrides)
                }
                base.evaluate()
            } catch (failure: Throwable) {
                primaryFailure = failure
                throw failure
            } finally {
                val actions = listOf<() -> Unit>({ stopKoin() }) + cleanup.asReversed().toList()
                val failures = actions.mapNotNull { runCatching(it).exceptionOrNull() }
                cleanup.clear()
                overrides.clear()
                active = false
                if (failures.isNotEmpty()) {
                    val failure = primaryFailure ?: failures.first()
                    failures.filter { it !== failure }.forEach(failure::addSuppressed)
                    if (primaryFailure == null) throw failure
                }
            }
        }
    }

    private fun checkActive() = check(active) {
        "Configure resources inside the UiTestEnvironment block, not in a test constructor"
    }
}
