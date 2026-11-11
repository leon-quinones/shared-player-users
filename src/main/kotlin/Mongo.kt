package com.example

import com.mongodb.client.*
import io.ktor.server.application.*
import io.ktor.server.config.tryGetString

/**
 * Creates the client from application configuration. Ktor DI closes it on shutdown.
 */
fun Application.createMongoClient(): MongoClient {
    val user = environment.config.tryGetString("database.user")
    val password = environment.config.tryGetString("database.password")
    val host = environment.config.tryGetString("database.host") ?: "127.0.0.1"
    val port = environment.config.tryGetString("database.port") ?: "27017"
    val maxPoolSize = environment.config.tryGetString("database.maxPoolSize")?.toInt() ?: 20

    val credentials = user?.let { userVal -> password?.let { passwordVal -> "$userVal:$passwordVal@" } }.orEmpty()
    val uri = "mongodb://$credentials$host:$port/?maxPoolSize=$maxPoolSize&w=majority"

    return MongoClients.create(uri)
}
