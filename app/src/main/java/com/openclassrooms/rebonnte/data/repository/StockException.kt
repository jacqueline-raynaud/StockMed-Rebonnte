package com.openclassrooms.rebonnte.data.repository

/**
 *  Define error display based on information from [Firestore Errors]
 */
enum class StockErrorReason {
    PERMISSION,
    NETWORK,
    UNAVAILABLE,
    INSUFFICIENT_STOCK,
    UNKNOWN
}

/**
 * Custom class for quantity stock
 * indicates stock quantity
 */
class StockException(
    val reason: StockErrorReason,
    cause: Throwable? = null,
    val available: Int? = null
) : Exception(cause?.message, cause)
