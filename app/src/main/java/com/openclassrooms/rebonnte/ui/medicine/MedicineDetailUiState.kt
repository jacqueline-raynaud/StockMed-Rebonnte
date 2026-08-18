package com.openclassrooms.rebonnte.ui.medicine

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.openclassrooms.rebonnte.ui.model.HistoryUi
import com.openclassrooms.rebonnte.ui.model.MedicineUi

/** One medicine card: the medicine, and the page of history it displays. */
@Immutable
data class MedicineDetailUiState(
    val medicine: MedicineUi? = null,
    val histories: List<HistoryUi> = emptyList(),
    /** True when the database holds older entries than the ones read. */
    val hasMoreHistory: Boolean = false,
    val isLoading: Boolean = true,
    @StringRes val errorMessage: Int? = null
)
