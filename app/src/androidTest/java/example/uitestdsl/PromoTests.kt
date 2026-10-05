package example.uitestdsl

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.testing.registerTestResponse
import example.uitestdsl.graphql.ProductsQuery
import example.uitestdsl.testing.BaseUiTest
import example.uitestdsl.testing.PromoScreen
import org.junit.Rule
import org.junit.Test

// Feature-level wrapper: shared setup for scenarios with an expired promo code.
abstract class ExpiredPromoUiTest : BaseUiTest() {
    protected val expiredMessage = "Срок действия промокода истёк"
    @get:Rule
    val rule = makeUiTestRule {
        factory<AddPromocode> { AddPromocode { PromoResult.Rejected(expiredMessage) } }
    }
}

class KoinPromoTest : ExpiredPromoUiTest() {
    @Test fun expiredPromoShowsError() {
        PromoScreen(composeTestRule).apply {
            submit("EXPIRED")
            assertResult(expiredMessage)
        }
    }
}

class RestPromoTest : BaseUiTest() {
    @get:Rule val rule = makeUiTestRule {
        restMockServer {
            POST("/promocodes") returns 200 withBodyContaining "\"code\":\"EXPIRED\"" withJson
                """{"accepted":false,"message":"Срок действия промокода истёк"}"""
        }
    }

    @Test fun restErrorAppearsAtTheInput() {
        PromoScreen(composeTestRule).apply {
            submit("EXPIRED")
            assertResult("Срок действия промокода истёк")
        }
    }
}

@OptIn(ApolloExperimental::class)
class GraphQlProductsTest : BaseUiTest() {
    @get:Rule val rule = makeUiTestRule {
        mockGraphQl {
            registerTestResponse(
                ProductsQuery(),
                ProductsQuery.Data(products = listOf(ProductsQuery.Product("1", "Наушники"))),
            )
        }
    }

    @Test fun graphQlProductIsVisible() {
        PromoScreen(composeTestRule).apply {
            loadProducts()
            assertProducts("Наушники")
        }
    }
}

class DisabledPromoTest : BaseUiTest() {
    @get:Rule val rule = makeUiTestRule {
        mockFeatureFlags { set(Flags.PROMOCODE_ENABLED, false) }
    }

    @Test fun disabledFlagHidesPromoActions() {
        PromoScreen(composeTestRule).assertPromoDisabled()
    }
}
