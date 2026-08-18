package com.openclassrooms.rebonnte.ui.model

import androidx.compose.runtime.Immutable
import com.openclassrooms.rebonnte.data.model.AisleDto

@Immutable
data class AisleUi(
    val id: String,
    val name: String
)

fun AisleDto.toUi(): AisleUi = AisleUi(id = id, name = name)
