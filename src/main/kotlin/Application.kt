package com.example

import com.mongodb.client.MongoClient
import com.example.databases.MongoAdapter
import com.example.repositories.UserRepository
import io.ktor.server.application.*
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve
import org.bson.Document

suspend fun Application.module() {
    val databaseName = environment.config.property("database.name").getString()
    dependencies {
        provide<MongoClient> { this@module.createMongoClient() }
        provide<UserRepository> {
            UserRepository(MongoAdapter(resolve<MongoClient>(), databaseName, "users", Document::class.java))
        }
    }

    configureSerialization()
    configureRouting(dependencies.resolve<UserRepository>())
}
