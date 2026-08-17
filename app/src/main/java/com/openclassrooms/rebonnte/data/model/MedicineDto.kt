package com.openclassrooms.rebonnte.data.model

import androidx.annotation.Keep

/**
 * The identifier corresponds to the one in Firestore.
 * This allows for medications with the same name and enables medication corrections without breaking references.
 *
 * The same applies to the radius, which can be modified without detaching the drug.
 *
 */
@Keep
data class MedicineDto(
    val id: String = "",
    val name: String = "",
    val stock: Int = 0,
    val aisleId: String = ""
)
