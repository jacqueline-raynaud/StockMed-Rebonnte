package com.openclassrooms.rebonnte.data.repository

import com.openclassrooms.rebonnte.data.model.UserDto
import kotlinx.coroutines.flow.Flow

/**
 * Acces au compte de l'operateur connecte.
 *
 * Les operations renvoient un [Result] plutot que de lever : une erreur
 * d'authentification (mot de passe faux, reseau absent) est un cas de
 * fonctionnement normal a afficher, pas un incident technique.
 */
interface UserRepository {

    /** Emet a chaque connexion ou deconnexion. `null` si personne n'est connecte. */
    val currentUser: Flow<UserDto?>

    /** Lecture synchrone, pour les cas ou l'on ne peut pas attendre le flux. */
    fun currentUserOrNull(): UserDto?

    suspend fun signIn(email: String, password: String): Result<UserDto>

    suspend fun signUp(email: String, password: String, displayName: String): Result<UserDto>

    fun signOut()
}
