package com.example.volunteering.data.model

data class Event(
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val participants: Int? = null,
    val type: String = "",
    val location: String = "",
    val imageUrl: String = "",
    val distance: Double? = null,
    val creatorUid: String = "",
    val interestedUsers: List<String> = emptyList(),
    val goingUsers: List<String> = emptyList()
)
