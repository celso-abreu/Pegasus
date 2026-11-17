package com.flutterforge.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PubspecParserTest {

    @Test
    fun `parses flutter pubspec fields and dependencies`() {
        val yaml = """
            name: flutter_forge_demo
            description: "A sample Flutter app"
            version: 1.2.3+4

            environment:
              sdk: ">=3.3.0 <4.0.0"

            dependencies:
              flutter:
                sdk: flutter
              http: ^1.2.1
              provider: ^6.1.2

            dev_dependencies:
              flutter_test:
                sdk: flutter
              flutter_lints: ^4.0.0

            flutter:
              uses-material-design: true
        """.trimIndent()

        val parsed = PubspecParser.parse(yaml)

        assertEquals("flutter_forge_demo", parsed.name)
        assertEquals("A sample Flutter app", parsed.description)
        assertEquals("1.2.3+4", parsed.version)
        assertEquals(">=3.3.0 <4.0.0", parsed.sdkConstraint)
        assertTrue(parsed.usesFlutter)
        assertEquals(3, parsed.dependencies.size)
        assertEquals("sdk: flutter", parsed.dependencies.first { it.name == "flutter" }.version)
        assertEquals("^1.2.1", parsed.dependencies.first { it.name == "http" }.version)
        assertEquals(2, parsed.devDependencies.size)
    }
}
