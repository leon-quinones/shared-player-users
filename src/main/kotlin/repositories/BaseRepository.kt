package com.example.interfaces

import kotlin.uuid.Uuid

interface BaseRepository<T> {
    fun getById(id: Uuid ): T?
    fun update(item: T): Boolean
    fun delete(id: String): Boolean
    fun add(item: T): Boolean
}