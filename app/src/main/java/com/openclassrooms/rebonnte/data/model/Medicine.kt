package com.openclassrooms.rebonnte.data.model

data class Medicine(
    var name: String,
    var stock: Int,
    var nameAisle: String,
    var histories: List<History>
)