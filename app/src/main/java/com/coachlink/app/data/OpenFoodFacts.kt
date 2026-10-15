package com.coachlink.app.data

import com.coachlink.app.domain.MacroValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class FoodLookup(val name: String, val barcode: String, val per100: MacroValues)

class OpenFoodFactsRepository {
    /** Lookup only: camera/scan cannot directly determine nutrients without a food database. */
    suspend fun lookup(rawCode: String): Result<FoodLookup> = withContext(Dispatchers.IO) {
        runCatching {
            val code = rawCode.trim()
            require(code.matches(Regex("[0-9]{8,14}"))) { "Code-barres non valide (8 à 14 chiffres)" }
            val url = URL("https://world.openfoodfacts.org/api/v2/product/$code.json?fields=code,product_name,product_name_fr,nutriments")
            val c = url.openConnection() as HttpURLConnection
            try {
                c.connectTimeout = 8000
                c.readTimeout = 8000
                c.setRequestProperty("User-Agent", "CoachLink/0.1 (contact: configure-before-production@example.org)")
                c.setRequestProperty("Accept", "application/json")
                check(c.responseCode == 200) { "Service alimentaire indisponible (HTTP ${c.responseCode})" }
                val data = c.inputStream.bufferedReader().use { it.readText() }
                val body = JSONObject(data)
                check(body.optInt("status") == 1) { "Aliment absent d'Open Food Facts" }
                val product = body.optJSONObject("product") ?: error("Fiche aliment absente")
                val nutrients = product.optJSONObject("nutriments") ?: error("Valeurs nutritionnelles manquantes")
                fun read(name: String): Double? = if (nutrients.has(name) && !nutrients.isNull(name)) {
                    nutrients.optDouble(name, Double.NaN).takeIf { it.isFinite() && it >= 0 }
                } else null
                val kcal = read("energy-kcal_100g") ?: read("energy-kj_100g")?.div(4.184)
                    ?: error("Calories par 100 g indisponibles")
                val protein = read("proteins_100g") ?: error("Protéines indisponibles")
                val carbs = read("carbohydrates_100g") ?: error("Glucides indisponibles")
                val fat = read("fat_100g") ?: error("Lipides indisponibles")
                val name = product.optString("product_name_fr").ifBlank { product.optString("product_name") }
                    .ifBlank { "Produit $code" }
                FoodLookup(name, code, MacroValues(kcal, protein, carbs, fat))
            } finally { c.disconnect() }
        }
    }
}
