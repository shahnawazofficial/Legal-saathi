package com.example.legalhelpaiapp

data class Resource(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val phone: String? = null,
    val website: String? = null,
    val iconRes: Int = R.drawable.ic_gavel,
    var isBookmarked: Boolean = false
)