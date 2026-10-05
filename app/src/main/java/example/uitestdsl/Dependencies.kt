package example.uitestdsl

import android.app.Application
import com.apollographql.apollo.ApolloClient
import example.uitestdsl.graphql.ProductsQuery
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import org.koin.core.context.startKoin
import org.koin.dsl.module
import retrofit2.http.Body
import retrofit2.http.POST

sealed interface PromoResult {
    data object Accepted : PromoResult
    data class Rejected(val message: String) : PromoResult
}

fun interface AddPromocode { suspend operator fun invoke(code: String): PromoResult }
fun interface GetProducts { suspend operator fun invoke(): List<String> }
fun interface FeatureFlags { fun isEnabled(name: String): Boolean }

object Flags { const val PROMOCODE_ENABLED = "promocode_enabled" }

interface PromoApi {
    @POST("promocodes")
    suspend fun add(@Body body: RequestBody): ResponseBody
}

class RestAddPromocode(private val api: PromoApi) : AddPromocode {
    override suspend fun invoke(code: String): PromoResult {
        val body = JSONObject().put("code", code).toString()
        val request = body.toRequestBody("application/json".toMediaType())
        val json = api.add(request).use { JSONObject(it.string()) }
        return if (json.getBoolean("accepted")) PromoResult.Accepted
        else PromoResult.Rejected(json.getString("message"))
    }
}

class GraphQlGetProducts(private val client: ApolloClient) : GetProducts {
    override suspend fun invoke(): List<String> =
        client.query(ProductsQuery()).execute().dataOrThrow().products.map { it.title }
}

// Offline defaults make the installed demo usable without accounts or external services.
fun demoModule() = module {
    single<FeatureFlags> { FeatureFlags { it == Flags.PROMOCODE_ENABLED } }
    factory<AddPromocode> {
        AddPromocode { code ->
            if (code == "EXPIRED") PromoResult.Rejected("Срок действия промокода истёк")
            else PromoResult.Accepted
        }
    }
    factory<GetProducts> { GetProducts { listOf("Демонстрационный товар") } }
}

class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin { modules(demoModule()) }
    }
}
