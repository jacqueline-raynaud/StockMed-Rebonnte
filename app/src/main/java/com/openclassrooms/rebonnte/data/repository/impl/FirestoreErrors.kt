package com.openclassrooms.rebonnte.data.repository.impl

import com.google.firebase.firestore.FirebaseFirestoreException
import com.openclassrooms.rebonnte.data.repository.StockErrorReason
import com.openclassrooms.rebonnte.data.repository.StockException
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

/**
 * Traduit une panne Firestore en [StockException].
 *
 * C'est le seul endroit du projet qui connait les codes d'erreur de Firestore.
 */
internal fun Throwable.toStockException(): StockException {
    if (this is StockException) return this

    val reason = when {
        this is FirebaseFirestoreException -> when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> StockErrorReason.PERMISSION

            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> StockErrorReason.NETWORK

            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED,
            FirebaseFirestoreException.Code.ABORTED -> StockErrorReason.UNAVAILABLE

            // FAILED_PRECONDITION couvre notamment l'index composite manquant :
            // c'est un defaut de configuration, pas un incident passager.
            else -> StockErrorReason.UNKNOWN
        }

        this is IOException -> StockErrorReason.NETWORK

        else -> StockErrorReason.UNKNOWN
    }
    return StockException(reason, this)
}

/**
 * Duree au-dela de laquelle on cesse d'attendre l'accuse de reception du
 * serveur. Assez longue pour qu'un reseau lent reponde, assez courte pour ne
 * pas laisser un operateur devant un ecran fige.
 */
private const val ACKNOWLEDGEMENT_TIMEOUT_MS = 5_000L

/**
 * Enveloppe une ecriture Firestore : n'en laisse sortir qu'une [StockException],
 * et **n'attend pas indefiniment** l'accuse de reception du serveur.
 *
 * Firestore applique toute ecriture au cache local immediatement, puis la
 * synchronise. `await()` attend la confirmation du serveur : hors ligne, elle
 * n'arrive jamais. L'ecran de creation restait donc fige sur son indicateur de
 * chargement alors que le medicament etait deja enregistre — un retour arriere
 * suffisait a le voir apparaitre.
 *
 * Passe ce delai, on considere l'ecriture acquise, ce qu'elle est : Firestore
 * la rejouera au retour du reseau. Les refus du serveur — droits insuffisants,
 * par exemple — continuent d'etre remontes quand il repond.
 */
internal suspend fun firestoreWrite(block: suspend () -> Unit) {
    try {
        withTimeoutOrNull(ACKNOWLEDGEMENT_TIMEOUT_MS) { block() }
    } catch (error: Exception) {
        throw error.toStockException()
    }
}

/**
 * Enveloppe une **transaction**, ou le silence du serveur est un echec.
 *
 * Contrairement a une ecriture simple, une transaction Firestore n'est jamais
 * appliquee localement : elle relit les documents cote serveur pour garantir
 * l'atomicite. Sans reponse, elle n'a pas eu lieu.
 *
 * Traiter son delai d'attente comme un succes annoncerait a l'operateur un
 * mouvement de stock qui ne s'est pas produit — exactement le genre d'ecart
 * que le service qualite ne peut pas rattraper apres coup.
 */
internal suspend fun firestoreTransaction(block: suspend () -> Unit) {
    try {
        withTimeoutOrNull(ACKNOWLEDGEMENT_TIMEOUT_MS) { block() }
            ?: throw StockException(StockErrorReason.UNAVAILABLE)
    } catch (error: Exception) {
        throw error.toStockException()
    }
}
