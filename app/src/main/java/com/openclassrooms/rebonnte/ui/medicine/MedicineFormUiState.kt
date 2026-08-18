package com.openclassrooms.rebonnte.ui.medicine

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

/**
 * The medicine form, for both creation and correction.
 *
 * [stock] is a String because it mirrors what is typed, including the empty
 * field. An Int could not represent a field being cleared.
 */
@Immutable
data class MedicineFormUiState(
    val name: String = "",
    val stock: String = "0",
    val aisleId: String = "",
    @StringRes val nameError: Int? = null,
    @StringRes val stockError: Int? = null,
    @StringRes val aisleError: Int? = null,
    @StringRes val submitError: Int? = null,
    val isSubmitting: Boolean = false,
    val isSaved: Boolean = false,
    val isEditing: Boolean = false
)
