package com.example

import com.example.model.User
import com.example.repositories.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.uuid.Uuid

fun Application.configureRouting(users: UserRepository) {
    routing {
        get("/") {
            call.respondText("Hello, World!")
        }
        get("/json/kotlinx-serialization") {
            call.respond(mapOf("hello" to "world"))
        }

        route("/users") {
            post {
                val input = call.receive<CreateUserRequest>()
                val user = User(
                    id = Uuid.random(),
                    userName = input.userName,
                    name = input.name,
                    avatarUrl = input.avatarUrl,
                    profileUrl = input.profileUrl,
                    email = input.email
                )
                if (!users.add(user)) {
                    call.respondText("User ID already exists", status = HttpStatusCode.Conflict)
                    return@post
                }

                call.response.header(HttpHeaders.Location, "/users/${user.id}")
                call.respond(HttpStatusCode.Created, user)
            }

            route("/{id}") {
                get {
                    call.respondUser(users.getById(call.userId()))
                }

                patch {
                    val id = call.userId()
                    val fields = call.receive<JsonObject>().userChanges()
                    call.respondUser(users.patch(id, fields))
                }
            }
        }
    }
}

@Serializable
private data class CreateUserRequest(
    val userName: String,
    val name: String,
    val avatarUrl: String,
    val profileUrl: String,
    val email: String? = null
)

private fun ApplicationCall.userId(): Uuid =
    parameters["id"]?.let(Uuid::parseOrNull) ?: throw BadRequestException("Invalid user ID")

private suspend fun ApplicationCall.respondUser(user: User?) {
    if (user == null) {
        respondText("User not found", status = HttpStatusCode.NotFound)
    } else {
        respond(user)
    }
}

private val editableUserFields = setOf("userName", "name", "avatarUrl", "profileUrl", "email")

private fun JsonObject.userChanges(): Map<String, String?> {
    if (isEmpty() || keys.any { it !in editableUserFields }) {
        throw BadRequestException("Provide at least one editable user field")
    }

    return mapValues { (field, value) ->
        when {
            field == "email" && value == JsonNull -> null
            value is JsonPrimitive && value.isString -> value.content
            else -> throw BadRequestException("Field '$field' must be a string")
        }
    }
}
