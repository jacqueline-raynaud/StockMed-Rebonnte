package com.openclassrooms.rebonnte.ui.model

import androidx.compose.runtime.Immutable
import com.openclassrooms.rebonnte.data.model.MedicineDto

@Immutable
data class MedicineUi(
    val id: String,
    val name: String,
    val stock: Int,
    val aisleId: String,
    val locationName: String?
)

fun MedicineDto.toUi(locationName: String?): MedicineUi = MedicineUi(
    id = id,
    name = name,
    stock = stock,
    aisleId = aisleId,
    locationName = locationName
)
