package com.openclassrooms.rebonnte.ui.aisle

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.openclassrooms.rebonnte.ui.model.AisleUi

/** The list of storage locations, or the fact that it is loading or failed. */
@Immutable
data class AisleUiState(
    val aisles: List<AisleUi> = emptyList(),
    val isLoading: Boolean = true,
    @StringRes val errorMessage: Int? = null
)
