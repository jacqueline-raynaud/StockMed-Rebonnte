package com.openclassrooms.rebonnte.data.repository

import com.openclassrooms.rebonnte.data.model.UserDto
import kotlinx.coroutines.flow.Flow


interface UserRepository {

    val currentUser: Flow<UserDto?>

    fun currentUserOrNull(): UserDto?

    suspend fun signIn(email: String, password: String): Result<UserDto>

    suspend fun signUp(email: String, password: String, displayName: String): Result<UserDto>

    fun signOut()

    suspend fun deleteAccount(password: String): Result<Unit>
}
