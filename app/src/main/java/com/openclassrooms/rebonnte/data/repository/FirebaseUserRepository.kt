package com.openclassrooms.rebonnte.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.openclassrooms.rebonnte.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseUserRepository @Inject constructor(
    private val auth: FirebaseAuth
) : UserRepository {

    /**
     * callbackFlow + awaitClose : l'ecouteur est retire des que plus personne
     * ne collecte. Un addAuthStateListener sans retrait symetrique retiendrait
     * son observateur pour toute la duree du processus.
     */
    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toUser())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun currentUserOrNull(): User? = auth.currentUser?.toUser()

    override suspend fun signIn(email: String, password: String): Result<User> = runCatching {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        checkNotNull(result.user).toUser()
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String
    ): Result<User> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val firebaseUser = checkNotNull(result.user)

        // Le nom d'affichage n'est pas un champ du compte a la creation : il
        // faut une mise a jour de profil, puis un reload pour que l'instance
        // locale le refletent.
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
}

private fun FirebaseUser.toUser(): User {
    val address = email.orEmpty()
    return User(
        id = uid,
        email = address,
        // Repli sur la partie locale de l'e-mail : un compte cree hors de
        // l'application peut ne pas avoir de nom d'affichage.
        displayName = displayName?.takeIf { it.isNotBlank() }
            ?: address.substringBefore('@')
    )
}
