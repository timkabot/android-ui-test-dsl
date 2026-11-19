plugins { kotlin("jvm") version "2.3.21" }
kotlin { jvmToolchain(17) }
dependencies { testImplementation("io.github.timkabot:ui-test-dsl:0.1.1") }
