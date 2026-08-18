package com.openclassrooms.rebonnte.ui.medicine

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.openclassrooms.rebonnte.data.repository.MedicineSort
import com.openclassrooms.rebonnte.ui.model.MedicineUi

/**
 * The medicine list screen: the result, and the search and sort that produced
 * it.
 *
 * The criterion travels with the list because the menu ticks the active one:
 * an unexposed criterion could not be shown.
 */
@Immutable
data class MedicineUiState(
    val medicines: List<MedicineUi> = emptyList(),
    val sort: MedicineSort = MedicineSort.NONE,
    val query: String = "",
    val isLoading: Boolean = true,
    @StringRes val errorMessage: Int? = null
)
