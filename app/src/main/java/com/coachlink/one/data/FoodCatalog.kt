package com.coachlink.one.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Open Food Facts: labelled packaged products, not a verified database of home-cooked meals. */
data class FoodSuggestion(
    val name: String,
    val brand: String,
    val kcal100: Double,
    val protein100: Double,
    val carbs100: Double,
    val fat100: Double,
    val source: String = "Open Food Facts"
)

object FoodCatalog {
    suspend fun search(name: String): Result<List<FoodSuggestion>> = withContext(Dispatchers.IO) {
        runCatching {
            require(name.trim().length in 3..80) { "Saisis au moins 3 caractères" }
            val encoded = Uri.encode(name.trim())
            // v1 full-text search (v2 structured search does not accept search_terms).
            val endpoint = "https://world.openfoodfacts.org/cgi/search.pl?search_terms=$encoded" +
                "&search_simple=1&action=process&json=1&page_size=12&fields=product_name,product_name_fr,brands,nutriments,code"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 9000
                setRequestProperty("User-Agent", "CoachLinkOne/1.1 (https://github.com/sulliv4nx/coachlink)")
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (conn.responseCode != 200) error("Recherche indisponible (HTTP ${conn.responseCode})")
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val products = JSONObject(json).optJSONArray("products") ?: return@runCatching emptyList()
                buildList {
                    for (i in 0 until products.length()) {
                        val item = products.optJSONObject(i) ?: continue
                        val n = item.optJSONObject("nutriments") ?: continue
                        val title = item.optString("product_name_fr").ifBlank { item.optString("product_name") }.trim()
                        if (title.length < 2) continue
                        val kcal = n.optDouble("energy-kcal_100g", Double.NaN).let {
                            if (it.isFinite()) it else n.optDouble("energy_100g", Double.NaN) / 4.184
                        }
                        val p = n.optDouble("proteins_100g", Double.NaN)
                        val c = n.optDouble("carbohydrates_100g", Double.NaN)
                        val f = n.optDouble("fat_100g", Double.NaN)
                        // Never replace unknown macros with zero; discard incomplete labels.
                        if (!kcal.isFinite() || kcal !in 0.0..1000.0 ||
                            listOf(p,c,f).any { !it.isFinite() || it !in 0.0..100.0 }) continue
                        add(FoodSuggestion(title, item.optString("brands"), kcal, p, c, f))
                    }
                }.distinctBy { it.name.lowercase() to it.brand.lowercase() }.take(8)
            } finally { conn.disconnect() }
        }
    }
}
