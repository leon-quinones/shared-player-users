package com.example.repositories

import com.example.databases.MongoAdapter
import com.example.interfaces.BaseRepository
import com.example.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import kotlin.uuid.Uuid

class UserRepository(private val adapter: MongoAdapter<Document>) : BaseRepository<User> {
    override suspend fun getById(id: Uuid): User? = withContext(Dispatchers.IO) {
        adapter.findById(id.toString())?.toUser()
    }

    override suspend fun add(item: User): Boolean = withContext(Dispatchers.IO) {
        adapter.insert(item.toDocument())
    }

    override suspend fun update(item: User): Boolean = withContext(Dispatchers.IO) {
        adapter.update(item.id.toString(), item.toDocument())
    }

    override suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
        adapter.delete(id)
    }

    suspend fun patch(id: Uuid, fields: Map<String, String?>): User? = withContext(Dispatchers.IO) {
        adapter.patch(id.toString(), Document("\$set", Document(fields)))?.toUser()
    }

    private fun User.toDocument(): Document = Document("_id", id.toString())
        .append("userName", userName)
        .append("name", name)
        .append("avatarUrl", avatarUrl)
        .append("profileUrl", profileUrl)
        .append("email", email)

    private fun Document.toUser(): User = User(
        id = Uuid.parse(getString("_id")),
        userName = getString("userName"),
        name = getString("name"),
        avatarUrl = getString("avatarUrl"),
        profileUrl = getString("profileUrl"),
        email = getString("email")
    )
}
