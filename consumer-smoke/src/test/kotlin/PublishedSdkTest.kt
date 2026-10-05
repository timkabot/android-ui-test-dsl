import io.github.timkabot.uitestdsl.UiTestEnvironment
import io.github.timkabot.uitestdsl.mockFeatureFlags
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

class PublishedSdkTest {
    @get:Rule
    val environment = UiTestEnvironment(defaultModules = { emptyList() }) {
        val server = restMockServer { GET("/message") returns 200 withJson "hello from SDK" }
        dependencies { single { server.url("/message") } }
    }

    @Test fun publishedArtifactWorksWithoutTheDemoApp() {
        val client = OkHttpClient()
        val url = GlobalContext.get().get<okhttp3.HttpUrl>()
        client.newCall(Request.Builder().url(url).build()).execute().use {
            assertEquals("hello from SDK", it.body.string())
        }
        assertTrue(mockFeatureFlags { set("example", true) }.isEnabled("example"))
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}
