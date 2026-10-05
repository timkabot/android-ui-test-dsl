package example.uitestdsl.testing

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class RestMockDslTest {
    private val client = OkHttpClient.Builder().callTimeout(3, TimeUnit.SECONDS).build()

    @Test fun routesByMethodExactPathAndBodyWithoutConsumingRequest() {
        MockWebServer().use { server ->
            server.dispatcher = RestMockDsl().apply {
                POST("/promo") returns 200 withBodyContaining "EXPIRED" withJson "expired"
                POST("/promo") returns 201 withBodyContaining "VALID" withJson "accepted"
            }.dispatcher()
            server.start()
            fun status(path: String, body: String): Int = client.newCall(
                Request.Builder().url(server.url(path)).post(body.toRequestBody()).build(),
            ).execute().use { it.code }
            assertEquals(201, status("/promo", "VALID"))
            assertEquals("VALID", server.takeRequest(1, TimeUnit.SECONDS)!!.body.readUtf8())
            assertEquals(404, status("/promo/other", "VALID"))
            assertEquals(404, status("/promo", "UNKNOWN"))
            client.newCall(Request.Builder().url(server.url("/promo")).build()).execute().use {
                assertEquals(404, it.code)
            }
        }
    }

    @Test fun sequenceFallsBackToConfiguredBodyAfterExhaustion() {
        MockWebServer().use { server ->
            server.dispatcher = RestMockDsl().apply {
                (GET("/items") returns 200 withJson "fallback").withJsonSequence("first", "second")
            }.dispatcher()
            server.start()
            fun body() = client.newCall(Request.Builder().url(server.url("/items")).build())
                .execute().use { it.body.string() }
            assertEquals(listOf("first", "second", "fallback"), List(3) { body() })
        }
    }
}
