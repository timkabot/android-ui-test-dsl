import io.github.timkabot.uitestdsl.UiTestEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun interface MessageSource {
    fun message(): String
}

class MessagePresenter(private val source: MessageSource) {
    fun text(): String = source.message()
}

class DependencyOverrideTest {
    @get:Rule
    val environment = UiTestEnvironment(
        defaultModules = {
            listOf(module {
                single<MessageSource>(named("primary")) { MessageSource { "default" } }
                single<MessageSource>(named("secondary")) { MessageSource { "secondary" } }
                factory { MessagePresenter(get(named("primary"))) }
            })
        },
    ) {
        dependencies {
            single<MessageSource>(named("primary")) { MessageSource { "test response" } }
        }
    }

    @Test
    fun existingConsumerUsesOverrideAndOtherBindingsStayAvailable() {
        val koin = GlobalContext.get()
        val first = koin.get<MessagePresenter>()
        val second = koin.get<MessagePresenter>()

        assertEquals("test response", first.text())
        assertNotSame(first, second)
        assertEquals("secondary", koin.get<MessageSource>(named("secondary")).message())
        assertSame(
            koin.get<MessageSource>(named("primary")),
            koin.get<MessageSource>(named("primary")),
        )
    }
}
