package com.openclassrooms.rebonnte.data.model

/** Nature de l'operation tracee dans l'historique. */
enum class HistoryAction {
    CREATE,
    UPDATE,
    STOCK_CHANGE,
    DELETE
}
