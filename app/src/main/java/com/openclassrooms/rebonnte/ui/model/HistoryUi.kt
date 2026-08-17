package com.openclassrooms.rebonnte.ui.model

import androidx.compose.runtime.Immutable
import com.openclassrooms.rebonnte.data.model.HistoryDto
import java.text.DateFormat
import java.util.Date

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
