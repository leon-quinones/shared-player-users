package com.example.repositories

import com.example.databases.BaseAdapter
import com.example.interfaces.BaseRepository
import com.example.model.User
import kotlin.uuid.Uuid

class UserRepository (private val provider: BaseAdapter<User>)
    : BaseRepository<User> {


    override fun getById(id: Uuid): User? {
        TODO("Not yet implemented")
    }

    override fun update(item: User): Boolean {
        TODO("Not yet implemented")
    }

    override fun delete(id: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun add(item: User): Boolean {
        TODO("Not yet implemented")
    }
}