package io.github.timkabot.uitestdsl

import org.junit.Assert.*
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.koin.core.context.GlobalContext
import org.koin.dsl.module

class UiTestEnvironmentTest {
    private fun statement(action: () -> Unit) = object : Statement() {
        override fun evaluate() = action()
    }
    private val description = Description.createTestDescription(javaClass, "scenario")

    @Test fun overridesExistBeforeInnerRuleAndCleanupRunsAfterIt() {
        val events = mutableListOf<String>()
        val environment = UiTestEnvironment(defaultModules = { listOf(module { single { "default" } }) }) {
            dependencies { single { "override" } }
            onClose { events += "resource" }
        }
        val inner = TestRule { base, _ -> statement {
            assertEquals("override", GlobalContext.get().get<String>())
            events += "launch"
            try { base.evaluate() } finally { events += "activity closed" }
        } }
        environment.around(inner).apply(statement { events += "test" }, description).evaluate()
        assertEquals(listOf("launch", "test", "activity closed", "resource"), events)
        assertNull(GlobalContext.getOrNull())
    }

    @Test fun primaryFailureSurvivesAndEveryResourceIsClosedInReverseOrder() {
        val events = mutableListOf<Int>()
        val failure = AssertionError("UI assertion")
        val cleanupFailure = IllegalStateException("close")
        val environment = UiTestEnvironment(defaultModules = { emptyList() }) {
            onClose { events += 1 }
            onClose { events += 2; throw cleanupFailure }
        }
        val actual = runCatching { environment.apply(statement { throw failure }, description).evaluate() }.exceptionOrNull()
        assertSame(failure, actual)
        assertEquals(listOf(2, 1), events)
        assertEquals(listOf(cleanupFailure), failure.suppressed.toList())
        assertNull(GlobalContext.getOrNull())
    }

    @Test fun partialSetupFailureClosesResourcesAndDoesNotLaunchTest() {
        var closed = false
        var launched = false
        val failure = IllegalArgumentException("bad setup")
        val environment = UiTestEnvironment(defaultModules = { emptyList() }) {
            onClose { closed = true }
            throw failure
        }
        val actual = runCatching { environment.apply(statement { launched = true }, description).evaluate() }.exceptionOrNull()
        assertSame(failure, actual)
        assertTrue(closed)
        assertFalse(launched)
        assertNull(GlobalContext.getOrNull())
    }

    @Test fun failedCleanupFailsAnOtherwisePassingTest() {
        val failure = IllegalStateException("close failed")
        val environment = UiTestEnvironment(defaultModules = { emptyList() }) { onClose { throw failure } }
        assertSame(failure, runCatching { environment.apply(statement {}, description).evaluate() }.exceptionOrNull())
    }

    @Test fun repeatedEvaluationsCreateFreshBindingsAndResources() {
        var count = 0
        var closed = 0
        val environment = UiTestEnvironment(defaultModules = { emptyList() }) {
            val value = ++count
            dependencies { single { value } }
            onClose { closed++ }
        }
        repeat(2) { index ->
            environment.apply(statement { assertEquals(index + 1, GlobalContext.get().get<Int>()) }, description).evaluate()
        }
        assertEquals(2, closed)
    }

    @Test fun resourcesCannotStartOutsideEvaluation() {
        val environment = UiTestEnvironment(defaultModules = { emptyList() })
        assertTrue(runCatching { environment.restMockServer {} }.exceptionOrNull() is IllegalStateException)
    }

    @Test fun flagsSnapshotOverridesDefaultsWithoutMutatingThem() {
        val defaults = mutableMapOf("enabled" to true)
        val flags = mockFeatureFlags(defaults) { set("other", true) }
        defaults["enabled"] = false
        assertTrue(flags.isEnabled("enabled"))
        assertTrue(flags.isEnabled("other"))
        assertFalse(flags.isEnabled("unknown"))
        assertFalse(mockFeatureFlags { set("enabled", false) }.isEnabled("enabled"))
    }
}
