package com.openclassrooms.rebonnte.data.model

import androidx.annotation.Keep

/**
 * Note: enumeration stored as its constant name
 * tracking the nature of the operation in the history.
 */
@Keep
enum class HistoryAction {
    CREATE,
    UPDATE,
    STOCK_CHANGE,
    DELETE
}
