package com.flutterforge.domain.model

data class Branch(
    val name: String,
    val commitSha: String,
    val isProtected: Boolean
)
