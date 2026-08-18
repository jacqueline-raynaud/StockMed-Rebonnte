package com.openclassrooms.rebonnte.ui.medicine

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.openclassrooms.rebonnte.ui.model.MedicineUi

/**
 * A medicines read: the result, or the fact that it is still loading or failed.
 *
 * Shared by the full list — where it is combined with the search text and the
 * sort criterion into a [MedicineUiState] — and by the single-aisle screen,
 * which needs nothing else.
 */
@Immutable
data class MedicineListUiState(
    val medicines: List<MedicineUi> = emptyList(),
    val isLoading: Boolean = true,
    @StringRes val errorMessage: Int? = null
)
