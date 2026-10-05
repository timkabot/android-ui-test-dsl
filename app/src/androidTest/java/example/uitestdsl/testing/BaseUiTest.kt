package example.uitestdsl.testing

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.apollographql.apollo.ApolloClient
import example.uitestdsl.*
import io.github.timkabot.uitestdsl.FeatureFlagsDsl
import io.github.timkabot.uitestdsl.RestMockDsl
import io.github.timkabot.uitestdsl.UiTestEnvironment
import io.github.timkabot.uitestdsl.mockFeatureFlags as flagValues
import org.junit.rules.TestRule
import org.koin.core.module.Module
import retrofit2.Retrofit

/** App adapter: only this class knows our Activity, contracts and default bindings. */
abstract class BaseUiTest {
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    private lateinit var environment: UiTestEnvironment

    fun makeUiTestRule(configure: Module.() -> Unit = {}): TestRule {
        environment = UiTestEnvironment(defaultModules = { listOf(demoModule()) }) {
            dependencies(configure)
        }
        return environment.around(composeTestRule)
    }

    fun Module.restMockServer(block: RestMockDsl.() -> Unit) {
        val server = environment.restMockServer(block)
        val api = Retrofit.Builder().baseUrl(server.url("/"))
            .build().create(PromoApi::class.java)
        factory<AddPromocode> { RestAddPromocode(api) }
    }

    fun Module.mockGraphQl(block: ApolloClient.() -> Unit) {
        val client = environment.mockGraphQl(block)
        factory<GetProducts> { GraphQlGetProducts(client) }
    }

    fun Module.mockFeatureFlags(block: FeatureFlagsDsl.() -> Unit) {
        val flags = flagValues(mapOf(Flags.PROMOCODE_ENABLED to true), block)
        single<FeatureFlags> { FeatureFlags(flags::isEnabled) }
    }
}
