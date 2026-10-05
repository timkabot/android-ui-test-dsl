package io.github.timkabot.uitestdsl

/** Immutable Boolean flag values. Adapt this object to your application's flag interface. */
class FeatureFlags internal constructor(private val values: Map<String, Boolean>) {
    fun isEnabled(name: String): Boolean = values[name] ?: false
}

@UiTestDsl
class FeatureFlagsDsl internal constructor(defaults: Map<String, Boolean>) {
    private val values = defaults.toMutableMap()
    fun set(name: String, enabled: Boolean) { values[name] = enabled }
    internal fun build(): FeatureFlags = FeatureFlags(values.toMap())
}

fun mockFeatureFlags(
    defaults: Map<String, Boolean> = emptyMap(),
    configure: FeatureFlagsDsl.() -> Unit,
): FeatureFlags = FeatureFlagsDsl(defaults).apply(configure).build()
