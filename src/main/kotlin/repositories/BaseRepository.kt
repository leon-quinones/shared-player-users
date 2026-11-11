package com.example.interfaces

import kotlin.uuid.Uuid

interface BaseRepository<T> {
    suspend fun getById(id: Uuid): T?
    suspend fun update(item: T): Boolean
    suspend fun delete(id: String): Boolean
    suspend fun add(item: T): Boolean
}
