package com.example.caloscan.domain

import com.example.caloscan.data.model.Food
import com.example.caloscan.util.normalizeForSearch

object FoodSearch {
  private val whitespace = Regex("\\s+")
  // Any letter (Cyrillic or Latin) or digit is part of a word
  private val nonWord = Regex("[^\\p{L}\\p{N}]+")

  /**
   * Returns the foods whose name, brand or English name contains every word of [text],
   * ignoring case and "ё"/"е" differences,
   * best matches first:
   *   0. the name starts with the search text
   *   1. a word in the name starts with the first search word
   *   2. the name contains the search text
   *   3. everything else
   * Ties are broken by shorter names first.
   */
  fun search(foods: List<Food>, text: String): List<Food> {
    val query = normalizeForSearch(text).trim()
    if (query.isEmpty()) return foods
    val tokens = query.split(whitespace).filter { it.isNotEmpty() }

    return foods
      .mapNotNull { food ->
        val name = normalizeForSearch(food.name)
        val haystack = name + " " + normalizeForSearch(food.brand) + " " + normalizeForSearch(food.searchTerms)
        if (!tokens.all { haystack.contains(it) }) return@mapNotNull null
        val score = when {
          name.startsWith(query) -> 0
          name.split(nonWord).any { it.startsWith(tokens[0]) } -> 1
          name.contains(query) -> 2
          else -> 3
        }
        food to score
      }
      .sortedWith(compareBy({ it.second }, { it.first.name.length }))
      .map { it.first }
  }
}
