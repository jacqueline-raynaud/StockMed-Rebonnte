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

    /**
     * callbackFlow + awaitClose : l'ecouteur est retire des que plus personne
     * ne collecte. Un addAuthStateListener sans retrait symetrique retiendrait
     * son observateur pour toute la duree du processus.
     */
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

    /**
     * Re-authentification puis suppression, dans cet ordre.
     *
     * Firebase refuse `delete()` si la connexion n'est pas recente, avec une
     * erreur peu parlante. Redemander le mot de passe leve la contrainte et
     * sert de confirmation : c'est une operation irreversible.
     *
     * L'e-mail est relu sur le compte plutot que passe en parametre : il doit
     * correspondre a la session en cours, quoi qu'affiche l'ecran.
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
