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

    /**
     * Supprime le compte d'authentification de l'operateur connecte.
     *
     * Le mot de passe est redemande parce que Firebase exige une connexion
     * recente pour cette operation : sans cela, un telephone laisse deverrouille
     * suffirait a supprimer le compte de son proprietaire.
     *
     * **L'historique n'est pas efface.** Les entrees restent, avec l'adresse de
     * leur auteur : un journal d'audit dont on peut retirer son propre nom ne
     * vaut rien. Le sort de cette donnee personnelle relevera de la politique
     * RGPD de l'entreprise — voir la question ouverte dans la documentation.
     */
    suspend fun deleteAccount(password: String): Result<Unit>
}
