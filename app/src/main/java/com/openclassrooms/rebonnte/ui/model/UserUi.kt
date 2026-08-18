package com.openclassrooms.rebonnte.ui.model

import androidx.compose.runtime.Immutable
import com.openclassrooms.rebonnte.data.model.UserDto


@Immutable
data class UserUi(
    val email: String,
    val displayName: String
)

fun UserDto.toUi(): UserUi = UserUi(email = email, displayName = displayName)
