package com.example

import com.example.databases.BaseAdapter
import com.example.model.User
import com.example.repositories.UserRepository
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.bson.conversions.Bson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class UserRepositoryTest {
    private val adapter = RecordingUserAdapter()
    private val repository = UserRepository(adapter)
    private val user = User(
        id = Uuid.parse("b72d0a6e-534c-4c53-b4bf-7b062ea5d22d"),
        userName = "ana",
        name = "Ana",
        avatarUrl = "https://example.com/avatar",
        profileUrl = "https://example.com/profile",
        email = "ana@example.com"
    )

    @Test
    fun `getById queries the UUID string and maps all stored fields`() = runBlocking {
        adapter.document = storedUser()

        assertEquals(user, repository.getById(user.id))
        assertEquals(user.id.toString(), adapter.requestedId)
    }

    @Test
    fun `getById returns null when the user is missing`() = runBlocking {
        assertNull(repository.getById(user.id))
        assertEquals(user.id.toString(), adapter.requestedId)
    }

    @Test
    fun `getById accepts documents without an email field`() = runBlocking {
        adapter.document = storedUser().apply { remove("email") }

        assertEquals(user.copy(email = null), repository.getById(user.id))
    }

    @Test
    fun `getById preserves an explicitly null email`() = runBlocking {
        adapter.document = storedUser().append("email", null)

        assertEquals(user.copy(email = null), repository.getById(user.id))
    }

    @Test
    fun `add maps the user to a document with a string primary key`() = runBlocking {
        assertTrue(repository.add(user))
        assertEquals(storedUser(), adapter.writtenDocument)
    }

    @Test
    fun `add stores a null email`() = runBlocking {
        assertTrue(repository.add(user.copy(email = null)))
        assertEquals(storedUser().append("email", null), adapter.writtenDocument)
    }

    @Test
    fun `add returns false when the adapter rejects the insert`() = runBlocking {
        adapter.writeResult = false

        assertFalse(repository.add(user))
    }

    @Test
    fun `update forwards the primary key and replacement document`() = runBlocking {
        assertTrue(repository.update(user))
        assertEquals(user.id.toString(), adapter.requestedId)
        assertEquals(storedUser(), adapter.writtenDocument)
    }

    @Test
    fun `update returns false when the adapter reports failure`() = runBlocking {
        adapter.writeResult = false

        assertFalse(repository.update(user))
    }

    @Test
    fun `delete forwards the identifier and returns success`() = runBlocking {
        assertTrue(repository.delete(user.id.toString()))
        assertEquals(user.id.toString(), adapter.requestedId)
    }

    @Test
    fun `delete returns false when no document was deleted`() = runBlocking {
        adapter.writeResult = false

        assertFalse(repository.delete(user.id.toString()))
    }

    @Test
    fun `patch sends only supplied fields with a set operation and returns the updated user`() = runBlocking {
        adapter.document = storedUser().append("name", "Ana Maria").append("email", null)

        val result = repository.patch(user.id, mapOf("name" to "Ana Maria", "email" to null))

        assertEquals(user.id.toString(), adapter.requestedId)
        assertEquals(
            Document("\$set", Document("name", "Ana Maria").append("email", null)),
            adapter.changes
        )
        assertEquals(user.copy(name = "Ana Maria", email = null), result)
    }

    @Test
    fun `patch returns null when the user is missing`() = runBlocking {
        assertNull(repository.patch(user.id, mapOf("name" to "Ana Maria")))
        assertEquals(user.id.toString(), adapter.requestedId)
    }

    @Test
    fun `read failures propagate to the caller`() = runBlocking {
        val failure = IllegalStateException("Database unavailable")
        adapter.failure = failure

        assertSame(failure, assertFailsWith<IllegalStateException> { repository.getById(user.id) })
    }

    @Test
    fun `insert failures propagate to the caller`() = runBlocking {
        val failure = IllegalStateException("Database unavailable")
        adapter.failure = failure

        assertSame(failure, assertFailsWith<IllegalStateException> { repository.add(user) })
    }

    @Test
    fun `patch failures propagate to the caller`() = runBlocking {
        val failure = IllegalStateException("Database unavailable")
        adapter.failure = failure

        assertSame(
            failure,
            assertFailsWith<IllegalStateException> { repository.patch(user.id, mapOf("name" to "Ana Maria")) }
        )
    }

    private fun storedUser(): Document = Document("_id", user.id.toString())
        .append("userName", "ana")
        .append("name", "Ana")
        .append("avatarUrl", "https://example.com/avatar")
        .append("profileUrl", "https://example.com/profile")
        .append("email", "ana@example.com")

    private class RecordingUserAdapter : BaseAdapter<Document> {
        var requestedId: String? = null
        var writtenDocument: Document? = null
        var changes: Bson? = null
        var document: Document? = null
        var writeResult = true
        var failure: Exception? = null

        override suspend fun findById(id: String): Document? {
            failure?.let { throw it }
            requestedId = id
            return document
        }

        override suspend fun getAll(): Collection<Document> =
            error("UserRepository does not use getAll")

        override suspend fun insert(entity: Document): Boolean {
            failure?.let { throw it }
            writtenDocument = entity
            return writeResult
        }

        override suspend fun update(id: String, entity: Document): Boolean {
            requestedId = id
            writtenDocument = entity
            return writeResult
        }

        override suspend fun delete(id: String): Boolean {
            requestedId = id
            return writeResult
        }

        override suspend fun patch(id: String, changes: Bson): Document? {
            failure?.let { throw it }
            requestedId = id
            this.changes = changes
            return document
        }
    }
}
