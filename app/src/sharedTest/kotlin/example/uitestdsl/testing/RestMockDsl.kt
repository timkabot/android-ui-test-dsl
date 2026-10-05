package example.uitestdsl.testing

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

@DslMarker
annotation class UiTestDsl

@UiTestDsl
class RestMockDsl {
    private val rules = mutableListOf<ResponseRule>()

    fun GET(path: String) = add("GET", path)
    fun POST(path: String) = add("POST", path)

    private fun add(method: String, path: String): ResponseRule =
        ResponseRule(method, path).also { rules += it }

    fun dispatcher(): Dispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse =
            rules.firstOrNull { it.matches(request) }?.response()
                ?: MockResponse().setResponseCode(404).setBody("No mock: ${request.method} ${request.path}")
    }
}

@UiTestDsl
class ResponseRule(private val method: String, private val path: String) {
    private var status = 200
    private var body = ""
    private var bodyFragment: String? = null
    private val sequence = ArrayDeque<String>()

    infix fun returns(code: Int) = apply { require(code in 100..599); status = code }
    infix fun withJson(json: String) = apply { body = json }
    infix fun withBodyContaining(fragment: String) = apply { bodyFragment = fragment }
    fun withJsonSequence(vararg json: String) = apply { sequence.clear(); sequence.addAll(json) }

    internal fun matches(request: RecordedRequest): Boolean =
        request.method == method && request.path == path &&
            (bodyFragment?.let { request.body.clone().readUtf8().contains(it) } ?: true)

    @Synchronized
    internal fun response(): MockResponse = MockResponse().setResponseCode(status)
        .setHeader("Content-Type", "application/json")
        .setBody(sequence.removeFirstOrNull() ?: body)
}
