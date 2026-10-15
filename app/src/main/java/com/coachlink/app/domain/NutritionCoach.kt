package com.coachlink.app.domain

import java.time.LocalDate

data class DailyNutrition(val day: LocalDate, val energy: Double, val protein: Double)

data class NutritionReview(val daysRecorded: Int, val avgCalories: Double?, val avgProtein: Double?, val advice: List<String>)

/** Missing days are *not* interpreted as zero calories or a deliberate fast. */
fun reviewNutrition(today: LocalDate, target: MacroTarget, records: List<DailyNutrition>, aiMode: Boolean): NutritionReview {
    val last7 = records.filter { !it.day.isAfter(today) && !it.day.isBefore(today.minusDays(6)) }
    val byDay = last7.groupBy { it.day }
    val days = byDay.size
    if (days < 3) return NutritionReview(days, null, null, listOf(
        "Seuls $days jour(s) renseigné(s) sur 7 : complétez le journal pour permettre un ajustement fiable.",
        "Vous pouvez commencer votre menu de courses avec les aliments que vous consommez régulièrement."
    ))
    val meanCalories = last7.sumOf { it.energy } / days
    val meanProtein = last7.sumOf { it.protein } / days
    val advice = mutableListOf<String>()
    if (aiMode) {
        if (target.protein > 0 && meanProtein < target.protein * .8)
            advice += "Sur les jours saisis, les protéines sont sous la cible : regardez les portions et répartissez les sources protéiques sur les repas."
        if (target.protein > 0 && meanProtein > target.protein * 1.35)
            advice += "Les protéines déclarées dépassent sensiblement l'objectif : vérifiez les portions et la pertinence de la cible."
        if (target.calories > 0 && meanCalories < target.calories * .8)
            advice += "Apports énergétiques consignés inférieurs à l'objectif : vérifiez si tous les repas ont été saisis avant toute conclusion."
        if (target.calories > 0 && meanCalories > target.calories * 1.2)
            advice += "Apports consignés supérieurs à l'objectif : regardez la tendance et la satiété sans modifier brutalement l'alimentation."
        if (advice.isEmpty()) advice += "Les apports saisis sont relativement proches des cibles actuelles ; poursuivez le suivi pour examiner la tendance."
    } else advice += "Mode nutrition coach : les objectifs et les menus viennent de votre coach ; aucun ajustement IA n'est appliqué."
    advice += "Aucune cible calorique ou macro n'est modifiée sans validation explicite."
    return NutritionReview(days, meanCalories, meanProtein, advice)
}
