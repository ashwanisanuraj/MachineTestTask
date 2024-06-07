package com.xero.machinetesttask

data class UserLogin(
    val userId: String,
    val timestamp: Long,
    val month: Int, // Month field added
    val phoneNumber: String? // Phone number field added
)
