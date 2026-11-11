package com.example.databases

import com.mongodb.MongoWriteException
import com.mongodb.client.MongoClient
import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.ReturnDocument
import org.bson.conversions.Bson


class MongoAdapter<T: Any>(
    client: MongoClient,
    databaseName: String,
    collectionName: String,
    documentClass: Class<T>,
    ): BaseAdapter<T> {

    private val collection = client
        .getDatabase(databaseName)
        .getCollection(collectionName, documentClass)


    override suspend fun findById(id: String): T? {
        return collection.find(
            Filters.eq("_id", id)).firstOrNull()
    }

    override suspend fun getAll(): Collection<T> {
        return collection.find().toList()
    }

    override suspend fun insert(entity: T): Boolean {
        try {
            collection.insertOne(entity)
            return true
        } catch (ex: Exception) {
            if (ex is MongoWriteException && ex.code == 11000) return false
            throw ex
        }
    }

    fun patch(id: String, changes: Bson): T? =
        collection.findOneAndUpdate(
            Filters.eq("_id", id),
            changes,
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        )

    override suspend fun update(id: String, entity: T): Boolean {
        try {
            collection.replaceOne(
                Filters.eq("_id", id),
                entity,
                ReplaceOptions().upsert(true)
            )
            return true
        } catch (ex: Exception) {
            println(ex.message)
            return false
        }
    }

    override suspend fun delete(id: String): Boolean {
        try {
            val result = collection.deleteOne(
                Filters.eq("_id", id)
            )
            return result.deletedCount > 0
        } catch (ex: Exception) {
            println(ex.message)
            return false
        }
    }

}
