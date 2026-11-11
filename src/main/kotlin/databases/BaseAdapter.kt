package com.example.databases

import org.bson.conversions.Bson

interface BaseAdapter<T> {
    suspend fun findById(id: String): T?
    suspend fun getAll(): Collection<T>
    suspend fun insert(entity: T): Boolean
    suspend fun update(id: String, entity: T): Boolean
    suspend fun patch(id: String, changes: Bson): T?
    suspend fun delete(id: String): Boolean
}
