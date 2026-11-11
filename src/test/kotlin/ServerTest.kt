package com.example

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import kotlin.test.*

class ServerTest {

    @Test
    fun `test root endpoint`() = testApplication {
        environment {
            config = MapApplicationConfig(
                "database.host" to "127.0.0.1",
                "database.port" to "27017",
                "database.name" to "shared_player_test"
            ).apply {
                put("ktor.application.modules", listOf("com.example.ApplicationKt.module"))
            }
        }

        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Hello, World!", response.bodyAsText())
    }

}
