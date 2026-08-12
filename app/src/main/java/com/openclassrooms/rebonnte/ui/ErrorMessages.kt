package com.openclassrooms.rebonnte.ui

import androidx.annotation.StringRes
import com.openclassrooms.rebonnte.R
import com.openclassrooms.rebonnte.data.repository.StockErrorReason
import com.openclassrooms.rebonnte.data.repository.StockException

/**
 * Le libelle a montrer a l'operateur pour un echec donne.
 *
 * Les messages disent quoi faire, pas ce qui s'est casse : « Reconnectez-vous »,
 * « Verifiez votre reseau ». Un operateur devant un rayon ne peut rien faire
 * d'un code d'erreur, et le detail technique se lit dans les journaux.
 *
 * Une exception inconnue tombe sur le message generique plutot que d'etre
 * affichee telle quelle : un message d'exception est en anglais, non traduit,
 * et peut divulguer la structure de la base.
 */
@StringRes
fun Throwable.toMessageRes(): Int = when ((this as? StockException)?.reason) {
    StockErrorReason.PERMISSION -> R.string.error_permission
    StockErrorReason.NETWORK -> R.string.error_network
    StockErrorReason.UNAVAILABLE -> R.string.error_unavailable
    StockErrorReason.UNKNOWN, null -> R.string.error_generic
}
