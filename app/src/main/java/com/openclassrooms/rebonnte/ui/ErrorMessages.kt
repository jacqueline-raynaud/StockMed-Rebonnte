package com.openclassrooms.rebonnte.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.openclassrooms.rebonnte.R
import com.openclassrooms.rebonnte.data.repository.StockErrorReason
import com.openclassrooms.rebonnte.data.repository.StockException

/**
 * Un message a afficher, avec ses eventuels arguments.
 *
 * Un simple identifiant de ressource ne suffisait plus : « il ne reste que 10
 * unite(s) » a besoin du chiffre. Le ViewModel ne peut pas formater lui-meme —
 * il faudrait un Context, donc une dependance Android dans une classe qui doit
 * rester testable sans emulateur. Il transporte donc la valeur, l'ecran met en
 * forme.
 */
@Immutable
data class UiMessage(
    @StringRes val res: Int,
    val args: List<Any> = emptyList()
)

/**
 * Le message a montrer a l'operateur pour un echec donne.
 *
 * Les messages disent quoi faire, pas ce qui s'est casse : « Reconnectez-vous »,
 * « Verifiez votre reseau ». Un operateur devant un rayon ne peut rien faire
 * d'un code d'erreur, et le detail technique se lit dans les journaux.
 *
 * Une exception inconnue tombe sur le message generique plutot que d'etre
 * affichee telle quelle : un message d'exception est en anglais, non traduit,
 * et peut divulguer la structure de la base.
 */
fun Throwable.toUiMessage(): UiMessage {
    val failure = this as? StockException
    return when (failure?.reason) {
        StockErrorReason.PERMISSION -> UiMessage(R.string.error_permission)
        StockErrorReason.NETWORK -> UiMessage(R.string.error_network)
        StockErrorReason.UNAVAILABLE -> UiMessage(R.string.error_unavailable)

        StockErrorReason.INSUFFICIENT_STOCK -> UiMessage(
            res = R.string.error_insufficient_stock,
            args = listOf(failure.available ?: 0)
        )

        StockErrorReason.UNKNOWN, null -> UiMessage(R.string.error_generic)
    }
}

/**
 * Variante sans argument, pour les etats d'ecran.
 *
 * Un echec de **lecture** n'est jamais un stock insuffisant : seule une
 * ecriture peut l'etre. Ces messages n'ont donc pas d'argument a porter.
 */
@StringRes
fun Throwable.toMessageRes(): Int = toUiMessage().res
