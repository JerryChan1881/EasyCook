package com.example.easycook.data

data class Recipe(
    val id: Int,
    val title: String,
    val image: String,
    val cookingTime: Int,
    val ingredientList: List<Ingredient>,
    val description: String = ""
)