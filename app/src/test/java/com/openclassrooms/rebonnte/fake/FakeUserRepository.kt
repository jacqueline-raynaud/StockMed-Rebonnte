package com.openclassrooms.rebonnte.fake

import com.openclassrooms.rebonnte.data.model.User
import com.openclassrooms.rebonnte.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Double de test ecrit a la main plutot que genere par une bibliotheque de
 * mocks : le comportement est lisible, et les assertions portent sur des
 * proprietes plutot que sur des verifications d'appels.
 */
class FakeUserRepository(
    initialUser: User? = SIGNED_IN_USER
) : UserRepository {

    private val userFlow = MutableStateFlow(initialUser)

    /** Resultat renvoye par [signIn] et [signUp]. Modifiable par le test. */
    var authResult: Result<User> = Result.success(SIGNED_IN_USER)

    var signInCount = 0
        private set
    var signUpCount = 0
        private set
    var signOutCount = 0
        private set

    override val currentUser: Flow<User?> = userFlow

    override fun currentUserOrNull(): User? = userFlow.value

    override suspend fun signIn(email: String, password: String): Result<User> {
        signInCount++
        return authResult.onSuccess { userFlow.value = it }
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String
    ): Result<User> {
        signUpCount++
        return authResult.onSuccess { userFlow.value = it }
    }

    override fun signOut() {
        signOutCount++
        userFlow.value = null
    }

    companion object {
        val SIGNED_IN_USER = User(
            id = "uid-1",
            email = "operateur@rebonnte.fr",
            displayName = "Operateur"
        )
    }
}
