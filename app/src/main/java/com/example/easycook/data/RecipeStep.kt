package com.example.easycook.data

data class RecipeStep(
    val id: Long,
    val instruction: String,
    val isDone: Boolean = false
)