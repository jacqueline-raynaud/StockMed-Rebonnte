package com.openclassrooms.rebonnte.data.model

/**
 * Un operateur de l'application.
 *
 * [email] est la donnee affichee dans l'historique : c'est ce que le service
 * qualite sait relier a une personne, contrairement a [id] qui est l'UID
 * Firebase.
 */
data class UserDto(
    val id: String = "",
    val email: String = "",
    val displayName: String = ""
)
