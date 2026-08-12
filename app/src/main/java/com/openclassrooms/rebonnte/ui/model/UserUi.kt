package com.openclassrooms.rebonnte.ui.model

import androidx.compose.runtime.Immutable
import com.openclassrooms.rebonnte.data.model.UserDto

/**
 * L'operateur connecte, tel que l'ecran d'accueil l'affiche.
 *
 * L'UID Firebase du [UserDto] ne figure pas ici : aucun ecran ne l'affiche, et
 * un identifiant technique n'a rien a faire dans un objet de presentation.
 * C'est ce que le modele d'affichage apporte — il ne porte que ce qui est vu.
 */
@Immutable
data class UserUi(
    val email: String,
    val displayName: String
)

fun UserDto.toUi(): UserUi = UserUi(email = email, displayName = displayName)
