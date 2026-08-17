package com.openclassrooms.rebonnte.data.model

import androidx.annotation.Keep

/**
 *
 * History entry enabling the reconstruction of the detailed history.
 */
@Keep
data class HistoryDto(
    val id: String = "",
    val medicineId: String = "",
    val medicineName: String = "",
    val userEmail: String = "",
    val date: Long = 0L,
    val action: HistoryAction = HistoryAction.UPDATE,
    val stockBefore: Int = 0,
    val stockAfter: Int = 0,
    val details: String = ""
)
