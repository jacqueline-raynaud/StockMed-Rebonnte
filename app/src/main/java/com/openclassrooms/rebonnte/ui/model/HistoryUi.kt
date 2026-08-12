package com.openclassrooms.rebonnte.ui.model

import androidx.compose.runtime.Immutable
import com.openclassrooms.rebonnte.data.model.HistoryDto
import java.text.DateFormat
import java.util.Date

/**
 * Une entree d'historique telle que l'ecran l'affiche.
 *
 * [dateLabel] est deja formate. Le [HistoryDto] porte un horodatage epoch, qui
 * ne s'affiche pas tel quel : le formatage etait fait dans la composable, il
 * est desormais fait ici, une fois, hors de la recomposition.
 *
 * Null quand la date est absente, et [userEmail] reste vide quand l'auteur est
 * inconnu : les deux libelles de remplacement sont des ressources, resolues par
 * l'ecran.
 */
@Immutable
data class HistoryUi(
    val id: String,
    val medicineName: String,
    val userEmail: String,
    val dateLabel: String?,
    val stockBefore: Int,
    val stockAfter: Int,
    val details: String
)

fun HistoryDto.toUi(): HistoryUi = HistoryUi(
    id = id,
    medicineName = medicineName,
    userEmail = userEmail,
    dateLabel = formatDate(date),
    stockBefore = stockBefore,
    stockAfter = stockAfter,
    details = details
)

private fun formatDate(epochMillis: Long): String? =
    if (epochMillis == 0L) {
        null
    } else {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(epochMillis))
    }
