package com.coachlink.app.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coachlink.app.data.PhotoFood
import com.coachlink.app.data.decodeMealPhoto
import com.coachlink.app.domain.MacroValues
import com.coachlink.app.domain.scaleFood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private data class DraftIngredient(
    val name: String,
    val grams: String,
    val calories100: String,
    val protein100: String,
    val carbs100: String,
    val fat100: String,
    val confidence: String
) {
    fun toFoodOrNull(): PhotoFood? {
        val g = grams.trim().replace(',', '.').toDoubleOrNull() ?: return null
        val kcal = calories100.trim().replace(',', '.').toDoubleOrNull() ?: return null
        val p = protein100.trim().replace(',', '.').toDoubleOrNull() ?: return null
        val c = carbs100.trim().replace(',', '.').toDoubleOrNull() ?: return null
        val f = fat100.trim().replace(',', '.').toDoubleOrNull() ?: return null
        if (name.isBlank() || g !in 0.1..5000.0 ||
            listOf(kcal, p, c, f).any { !it.isFinite() || it < 0.0 } ||
            kcal > 900.0 || maxOf(p,c,f) > 100.0) return null
        return PhotoFood(name, g, MacroValues(kcal,p,c,f), confidence)
    }
}

private fun displayNumber(n: Double): String = String.format(Locale.FRANCE, "%.1f", n)

@Composable
fun PhotoMealDialog(vm: CoachViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val prediction by vm.photoEstimate.collectAsStateWithLifecycle()
    val analyzing by vm.photoLoading.collectAsStateWithLifecycle()
    val drafts = remember { mutableStateListOf<DraftIngredient>() }
    var uri by remember { mutableStateOf<Uri?>(null) }
    var imagePreview by remember { mutableStateOf<Bitmap?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    var allowUpload by remember { mutableStateOf(false) }
    var mealType by remember { mutableStateOf("Déjeuner") }
    var showNutritionValues by remember { mutableStateOf(false) }
    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { selected ->
        if (selected != null) { uri = selected; vm.clearPhotoEstimate(); allowUpload = false; drafts.clear() }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) {
            uri = pendingCameraUri
            vm.clearPhotoEstimate()
            allowUpload = false
            drafts.clear()
        } else { cameraFile?.delete() }
    }
    fun takePhoto() {
        runCatching {
            val folder = File(context.cacheDir, "photos").apply { mkdirs() }
            val file = File.createTempFile("repas_", ".jpg", folder)
            val photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraFile?.delete()
            cameraFile = file
            pendingCameraUri = photoUri
            camera.launch(photoUri)
        }.onFailure { vm.inform("Appareil photo indisponible : ${it.localizedMessage ?: "utilisez la galerie"}") }
    }
    DisposableEffect(Unit) { onDispose { cameraFile?.delete() } }
    LaunchedEffect(uri) {
        imagePreview = null
        uri?.let { selection ->
            runCatching { withContext(Dispatchers.IO) { decodeMealPhoto(context.contentResolver, selection, 700) } }
                .onSuccess { imagePreview = it }
                .onFailure { vm.inform("Impossible de prévisualiser cette photo") }
        }
    }
    LaunchedEffect(prediction) {
        drafts.clear()
        prediction?.foods?.forEach { item ->
            drafts.add(DraftIngredient(item.name, displayNumber(item.grams),
                displayNumber(item.per100.calories), displayNumber(item.per100.protein),
                displayNumber(item.per100.carbs), displayNumber(item.per100.fat), item.confidence))
        }
    }
    val corrected = drafts.map { it.toFoodOrNull() }
    val complete = corrected.isNotEmpty() && corrected.all { it != null }
    val sum = corrected.filterNotNull().map { scaleFood(it.per100, it.grams) }
    val calories = sum.sumOf { it.calories }
    val proteins = sum.sumOf { it.protein }
    val carbs = sum.sumOf { it.carbs }
    val fats = sum.sumOf { it.fat }

    AlertDialog(
        onDismissRequest = { if (!analyzing) onClose() },
        title = { Text("Analyser un repas en photo") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 600.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Photo de l'assiette, estimation des aliments, puis correction avant enregistrement.", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { takePhoto() }, modifier = Modifier.weight(1f), enabled = !analyzing) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(5.dp)); Text("Photo")
                    }
                    OutlinedButton(onClick = {
                        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }, modifier = Modifier.weight(1f), enabled = !analyzing) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(5.dp)); Text("Galerie")
                    }
                }
                imagePreview?.let { bmp ->
                    Image(bmp.asImageBitmap(), contentDescription = "Aperçu du repas sélectionné",
                        modifier = Modifier.fillMaxWidth().height(175.dp), contentScale = ContentScale.Fit)
                }
                if (!vm.photoConfigured) {
                    Text("Le service d'analyse IA n'est pas encore connecté. L'acquisition photo fonctionne, mais l'estimation requiert un serveur HTTPS configuré.",
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (uri != null && prediction == null) {
                    Row {
                        Checkbox(allowUpload, onCheckedChange = { allowUpload = it })
                        Text("J'autorise l'envoi de cette photo à un service d'analyse IA. Aucun envoi sans mon accord.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { uri?.let(vm::analyzeMealPhoto) },
                        enabled = allowUpload && vm.photoConfigured && !analyzing,
                        modifier = Modifier.fillMaxWidth()) {
                        Text(if (analyzing) "Analyse en cours…" else "Estimer les calories et les macros")
                    }
                    if (analyzing) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                prediction?.let { estimate ->
                    Text("Résultats estimés — à vérifier", style = MaterialTheme.typography.titleMedium)
                    if (estimate.notes.isNotBlank()) Text(estimate.notes, style = MaterialTheme.typography.bodySmall)
                    drafts.toList().forEachIndexed { index, food ->
                        HorizontalDivider()
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Aliment ${index+1} • fiabilité ${food.confidence}",
                                modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                            IconButton(onClick = { drafts.removeAt(index) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Retirer cet ingrédient")
                            }
                        }
                        DraftField("Aliment / ingrédient", food.name, { drafts[index] = food.copy(name = it) })
                        DraftField("Quantité estimée (g) : corrigez si nécessaire", food.grams,
                            { drafts[index] = food.copy(grams = it) }, KeyboardType.Decimal)
                        if (showNutritionValues) {
                            DraftField("Calories / 100 g", food.calories100,
                                { drafts[index] = food.copy(calories100 = it) }, KeyboardType.Decimal)
                            DraftField("Protéines / 100 g", food.protein100,
                                { drafts[index] = food.copy(protein100 = it) }, KeyboardType.Decimal)
                            DraftField("Glucides / 100 g", food.carbs100,
                                { drafts[index] = food.copy(carbs100 = it) }, KeyboardType.Decimal)
                            DraftField("Lipides / 100 g", food.fat100,
                                { drafts[index] = food.copy(fat100 = it) }, KeyboardType.Decimal)
                        }
                    }
                    TextButton(onClick = { showNutritionValues = !showNutritionValues }) {
                        Text(if (showNutritionValues) "Masquer les valeurs / 100 g" else "Corriger les valeurs nutritionnelles")
                    }
                    Text("Total estimé : ${displayNumber(calories)} kcal | P ${displayNumber(proteins)} g • G ${displayNumber(carbs)} g • L ${displayNumber(fats)} g")
                    Text("Attention : sauces, huile, ingrédients cachés et tailles de portions peuvent être mal estimés.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Type de repas : $mealType", style = MaterialTheme.typography.labelMedium)
                    listOf("Petit-déjeuner", "Déjeuner", "Dîner", "Collation").forEach { choice ->
                        FilterChip(selected = mealType == choice, onClick = { mealType = choice }, label = { Text(choice) })
                    }
                }
            }
        },
        confirmButton = {
            if (prediction != null) {
                Button(enabled = !analyzing && complete, onClick = {
                    if (vm.savePhotoFoods(corrected.filterNotNull(), mealType)) onClose()
                }) { Text("Valider et enregistrer") }
            }
        },
        dismissButton = { TextButton(onClick = onClose, enabled = !analyzing) { Text("Fermer") } }
    )
}

@Composable
private fun DraftField(label: String, value: String, onChange: (String) -> Unit,
                       keyboard: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        singleLine = true, modifier = Modifier.fillMaxWidth())
}
