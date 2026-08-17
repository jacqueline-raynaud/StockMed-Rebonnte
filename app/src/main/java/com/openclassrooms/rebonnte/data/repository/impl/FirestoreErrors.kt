package com.openclassrooms.rebonnte.data.repository.impl

import com.google.firebase.firestore.FirebaseFirestoreException
import com.openclassrooms.rebonnte.data.repository.StockErrorReason
import com.openclassrooms.rebonnte.data.repository.StockException
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException


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

            else -> StockErrorReason.UNKNOWN
        }

        this is IOException -> StockErrorReason.NETWORK

        else -> StockErrorReason.UNKNOWN
    }
    return StockException(reason, this)
}

/**
 * The duration after which the system stops waiting for an acknowledgment
 */
private const val ACKNOWLEDGEMENT_TIMEOUT_MS = 5_000L

/**
* The lack of an acknowledgment during a write operation is not considered like a failure.
* The write is stored in the local cache and will be retried upon reconnection.
 */
internal suspend fun firestoreWrite(block: suspend () -> Unit) {
    try {
        withTimeoutOrNull(ACKNOWLEDGEMENT_TIMEOUT_MS) { block() }
    } catch (error: Exception) {
        throw error.toStockException()
    }
}

/**
 * The lack of an acknowledgment during a transaction constitutes a failure.
 * transaction cannot be local
 */
internal suspend fun <T : Any> firestoreTransaction(block: suspend () -> T): T =
    try {
        withTimeoutOrNull(ACKNOWLEDGEMENT_TIMEOUT_MS) { block() }
            ?: throw StockException(StockErrorReason.UNAVAILABLE)
    } catch (error: Exception) {
        throw error.toStockException()
    }
