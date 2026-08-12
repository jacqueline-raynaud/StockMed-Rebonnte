package com.openclassrooms.rebonnte.fake

import com.openclassrooms.rebonnte.data.model.UserDto
import com.openclassrooms.rebonnte.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Double de test ecrit a la main plutot que genere par une bibliotheque de
 * mocks : le comportement est lisible, et les assertions portent sur des
 * proprietes plutot que sur des verifications d'appels.
 */
class FakeUserRepository(
    initialUser: UserDto? = SIGNED_IN_USER
) : UserRepository {

    private val userFlow = MutableStateFlow(initialUser)

    /** Resultat renvoye par [signIn] et [signUp]. Modifiable par le test. */
    var authResult: Result<UserDto> = Result.success(SIGNED_IN_USER)

    var signInCount = 0
        private set
    var signUpCount = 0
        private set
    var signOutCount = 0
        private set

    override val currentUser: Flow<UserDto?> = userFlow

    override fun currentUserOrNull(): UserDto? = userFlow.value

    override suspend fun signIn(email: String, password: String): Result<UserDto> {
        signInCount++
        return authResult.onSuccess { userFlow.value = it }
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String
    ): Result<UserDto> {
        signUpCount++
        return authResult.onSuccess { userFlow.value = it }
    }

    override fun signOut() {
        signOutCount++
        userFlow.value = null
    }

    /** Resultat renvoye par [deleteAccount]. Modifiable par le test. */
    var deleteResult: Result<Unit> = Result.success(Unit)

    var deleteAccountCount = 0
        private set

    /** Dernier mot de passe recu : le test verifie qu'il est bien transmis. */
    var lastDeletePassword: String? = null
        private set

    override suspend fun deleteAccount(password: String): Result<Unit> {
        deleteAccountCount++
        lastDeletePassword = password
        // La suppression du compte ferme la session, comme chez Firebase.
        return deleteResult.onSuccess { userFlow.value = null }
    }

    companion object {
        val SIGNED_IN_USER = UserDto(
            id = "uid-1",
            email = "operateur@rebonnte.fr",
            displayName = "Operateur"
        )
    }
}
