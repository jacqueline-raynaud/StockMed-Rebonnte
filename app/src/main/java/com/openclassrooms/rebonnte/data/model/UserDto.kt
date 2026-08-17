package com.openclassrooms.rebonnte.data.model

import androidx.annotation.Keep

@Keep
data class UserDto(
    val id: String = "",
    val email: String = "",
    val displayName: String = ""
)

