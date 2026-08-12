package com.openclassrooms.rebonnte.fake

import com.openclassrooms.rebonnte.data.model.UserDto
import com.openclassrooms.rebonnte.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remplace Firebase Authentication pendant les tests d'interface.
 *
 * [setSignedIn] permet d'installer une session **avant** le lancement de
 * l'Activity : c'est la seule facon de reproduire le demarrage avec une session
 * deja ouverte sans avoir a tuer le processus.
 */
@Singleton
class FakeUserRepository @Inject constructor() : UserRepository {

    private val userFlow = MutableStateFlow<UserDto?>(null)

    override val currentUser: Flow<UserDto?> = userFlow

    override fun currentUserOrNull(): UserDto? = userFlow.value

    override suspend fun signIn(email: String, password: String): Result<UserDto> {
        val user = UserDto(id = "uid-test", email = email, displayName = TEST_DISPLAY_NAME)
        userFlow.value = user
        return Result.success(user)
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String
    ): Result<UserDto> {
        val user = UserDto(id = "uid-test", email = email, displayName = displayName)
        userFlow.value = user
        return Result.success(user)
    }

    override fun signOut() {
        userFlow.value = null
    }

    fun setSignedIn(user: UserDto?) {
        userFlow.value = user
    }

    companion object {
        const val TEST_DISPLAY_NAME = "Operateur"

        val TEST_USER = UserDto(
            id = "uid-test",
            email = "operateur@rebonnte.fr",
            displayName = TEST_DISPLAY_NAME
        )
    }
}
