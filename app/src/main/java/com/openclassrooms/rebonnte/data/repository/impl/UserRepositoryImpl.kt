package com.openclassrooms.rebonnte.data.repository.impl

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.openclassrooms.rebonnte.data.model.UserDto
import com.openclassrooms.rebonnte.data.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : UserRepository {

    override val currentUser: Flow<UserDto?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toUser())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun currentUserOrNull(): UserDto? = auth.currentUser?.toUser()

    override suspend fun signIn(email: String, password: String): Result<UserDto> = runCatching {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        checkNotNull(result.user).toUser()
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String
    ): Result<UserDto> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val firebaseUser = checkNotNull(result.user)

        firebaseUser.updateProfile(
            UserProfileChangeRequest.Builder()
                .setDisplayName(displayName.trim())
                .build()
        ).await()
        firebaseUser.reload().await()

        checkNotNull(auth.currentUser).toUser()
    }

    override fun signOut() {
        auth.signOut()
    }

    /**
     * Re-authentification and delet account
     */
    override suspend fun deleteAccount(password: String): Result<Unit> = runCatching {
        val firebaseUser = checkNotNull(auth.currentUser)
        val credential = EmailAuthProvider.getCredential(
            checkNotNull(firebaseUser.email),
            password
        )
        firebaseUser.reauthenticate(credential).await()
        firebaseUser.delete().await()
    }
}

private fun FirebaseUser.toUser(): UserDto {
    val address = email.orEmpty()
    return UserDto(
        id = uid,
        email = address,
        // Repli sur la partie locale de l'e-mail : un compte cree hors de
        // l'application peut ne pas avoir de nom d'affichage.
        displayName = displayName?.takeIf { it.isNotBlank() }
            ?: address.substringBefore('@')
    )
}
