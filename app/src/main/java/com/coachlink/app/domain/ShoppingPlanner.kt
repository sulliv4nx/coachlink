package com.coachlink.app.domain

import java.text.Normalizer
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

data class PlannedIngredient(
    val dayOfWeek: Int, // Monday=1 ... Sunday=7
    val mealType: String,
    val food: String,
    val gramsPerServing: Double,
    val servings: Int = 1
)

data class GroceryLine(val key: String, val label: String, val grams: Double, val servings: Int)

data class GroceryList(val from: LocalDate, val until: LocalDate, val items: List<GroceryLine>, val plannedDays: Int)

fun groceryKey(name: String): String = Normalizer.normalize(name.lowercase().trim(), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").replace(Regex("\\s+"), " ")

/** Generate ONLY from explicitly planned meals. Never confuse logged consumption with future needs. */
fun buildGroceryList(plan: List<PlannedIngredient>, from: LocalDate, until: LocalDate): GroceryList {
    require(!until.isBefore(from) && ChronoUnit.DAYS.between(from, until) <= 366)
    require(plan.all { it.dayOfWeek in 1..7 && it.food.isNotBlank() &&
        it.gramsPerServing.isFinite() && it.gramsPerServing > 0 && it.gramsPerServing <= 5000 && it.servings in 1..20 })
    val lines = linkedMapOf<String, GroceryLine>()
    var date = from
    while (!date.isAfter(until)) {
        plan.filter { it.dayOfWeek == date.dayOfWeek.value }.forEach { food ->
            val key = groceryKey(food.food)
            val old = lines[key]
            lines[key] = GroceryLine(key, old?.label ?: food.food.trim(),
                (old?.grams ?: 0.0) + food.gramsPerServing * food.servings,
                (old?.servings ?: 0) + food.servings)
        }
        date = date.plusDays(1)
    }
    return GroceryList(from, until, lines.values.sortedBy { it.label.lowercase() },
        (ChronoUnit.DAYS.between(from, until) + 1).toInt())
}

fun gramsDisplay(amount: Double): String = if (amount >= 1000.0) {
    String.format(java.util.Locale.FRANCE, "%.2f kg", amount / 1000)
} else "${amount.roundToInt()} g"
