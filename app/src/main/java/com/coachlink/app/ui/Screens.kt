package com.coachlink.app.ui

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coachlink.app.data.*
import com.coachlink.app.domain.*
import com.google.android.gms.common.api.ApiException
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.time.LocalDate
import java.util.Locale

private val primary = Color(0xFF16635D)
private val tint = Color(0xFFF0F7F5)
private fun number(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()
private fun numberInt(s: String): Int? = s.trim().toIntOrNull()
private fun fmt(n: Double): String = String.format(Locale.FRANCE, "%.1f", n)
private fun Meal.nutrition() = scaleFood(MacroValues(calories100, protein100, carbs100, fat100), grams)
private fun totals(meals: List<Meal>) = MacroValues(
    meals.sumOf { it.nutrition().calories }, meals.sumOf { it.nutrition().protein },
    meals.sumOf { it.nutrition().carbs }, meals.sumOf { it.nutrition().fat }
)

@Composable
fun CoachLinkApp(vm: CoachViewModel) {
    val goalDb by vm.goal.collectAsStateWithLifecycle(initialValue = null)
    val meals by vm.meals.collectAsStateWithLifecycle(initialValue = emptyList())
    val programs by vm.programs.collectAsStateWithLifecycle(initialValue = emptyList())
    val exercises by vm.exercises.collectAsStateWithLifecycle(initialValue = emptyList())
    val logs by vm.logs.collectAsStateWithLifecycle(initialValue = emptyList())
    val metrics by vm.metrics.collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by vm.settings.collectAsStateWithLifecycle(initialValue = null)
    val extra by vm.extraWorkouts.collectAsStateWithLifecycle(initialValue = emptyList())
    val plan by vm.mealPlan.collectAsStateWithLifecycle(initialValue = emptyList())
    val food by vm.food.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val notice by vm.notice.collectAsStateWithLifecycle()
    var isCoach by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val today = LocalDate.now().toString()
    val goal = goalDb ?: Goal()
    val dayMeals = meals.filter { it.day == today }
    val dayLogs = logs.filter { it.day == today }
    val total = totals(dayMeals)
    val labels = listOf("Tableau", "Nutrition", "Sport", "Coach IA", "Courses")
    val icons = listOf(Icons.Default.Dashboard, Icons.Default.Restaurant, Icons.Default.FitnessCenter,
        Icons.Default.Psychology, Icons.Default.ShoppingCart)
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(notice) {
        notice?.let { snackbar.showSnackbar(it); vm.dismissNotice() }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Surface(color = primary) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("COACHLINK", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                        Text(if (isCoach) "Mode coach • démonstration locale" else "Espace athlète • démonstration locale",
                            style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                    }
                    TextButton(onClick = { isCoach = !isCoach; selected = 0 }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (isCoach) "Athlète" else "Coach")
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(selected = selected == index, onClick = { selected = index },
                        icon = { Icon(icons[index], contentDescription = label) }, label = { Text(label) })
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (selected) {
                0 -> {
                    WeeklyDashboardScreen(goal, meals, logs, metrics, isCoach)
                    HorizontalDivider()
                    Dashboard(goal, total, dayMeals, dayLogs, exercises, programs, metrics)
                }
                1 -> NutritionScreen(vm, goal, dayMeals, total, food, loading)
                2 -> SportScreen(vm, programs, exercises, logs, isCoach || (settings?.sportMode == "HUMAN"), today)
                3 -> {
                    AdaptiveCoachScreen(vm, settings ?: AthleteSettings(), goal, meals, logs, exercises, extra, metrics, isCoach)
                    if (isCoach || settings?.nutritionMode == "HUMAN") {
                        HorizontalDivider()
                        CoachScreen(vm, goal, total, dayLogs, metrics, programs, exercises, dayMeals)
                    }
                }
                4 -> GroceryScreen(vm, plan, meals)
            }
        }
    }
}

@Composable
private fun Panel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        })
    }
}

@Composable
private fun ProgressMetric(label: String, actual: Double, target: Double, unit: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("${fmt(actual)} / ${fmt(target)} $unit", style = MaterialTheme.typography.labelMedium)
        }
        LinearProgressIndicator(progress = { adherence(actual, target) }, modifier = Modifier.fillMaxWidth().height(8.dp), color = primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant)
    }
}

@Composable
private fun Dashboard(goal: Goal, total: MacroValues, todayMeals: List<Meal>, dayLogs: List<WorkoutLog>, exercises: List<Exercise>, programs: List<Program>, metrics: List<BodyMetric>) {
    Text("Vue du jour", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(LocalDate.now().toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
    Panel("Apports nutritionnels") {
        Text("${fmt(total.calories)} kcal", style = MaterialTheme.typography.headlineMedium, color = primary, fontWeight = FontWeight.Black)
        ProgressMetric("Énergie", total.calories, goal.calories.toDouble(), "kcal")
        ProgressMetric("Protéines", total.protein, goal.protein.toDouble(), "g")
        ProgressMetric("Glucides", total.carbs, goal.carbs.toDouble(), "g")
        ProgressMetric("Lipides", total.fat, goal.fat.toDouble(), "g")
    }
    Panel("Entraînement") {
        Text("${dayLogs.size} exercice(s) enregistré(s) aujourd'hui")
        Text("${programs.size} programme(s) • ${exercises.size} exercice(s) attribué(s)", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Volume du jour : ${fmt(dayLogs.sumOf { it.setsDone * it.repsDone * it.loadKg })} kg", fontWeight = FontWeight.Medium)
    }
    Panel("Suivi corporel") {
        Text(metrics.firstOrNull()?.let { "Dernière pesée : ${fmt(it.weightKg)} kg (${it.day})" } ?: "Aucune pesée enregistrée")
        Text("${todayMeals.size} aliment(s) enregistrés aujourd'hui", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text("Les calories dépensées à l'entraînement ne sont pas automatiquement déduites des apports alimentaires.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun NutritionScreen(vm: CoachViewModel, goal: Goal, dayMeals: List<Meal>, total: MacroValues, food: FoodLookup?, loading: Boolean) {
    var adding by remember { mutableStateOf(false) }
    var photoDialog by remember { mutableStateOf(false) }
    Text("Nutrition", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Panel("Objectifs journaliers") {
        ProgressMetric("Calories", total.calories, goal.calories.toDouble(), "kcal")
        ProgressMetric("Protéines", total.protein, goal.protein.toDouble(), "g")
        ProgressMetric("Glucides", total.carbs, goal.carbs.toDouble(), "g")
        ProgressMetric("Lipides", total.fat, goal.fat.toDouble(), "g")
    }
    Button(onClick = { vm.clearFood(); adding = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = primary)) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Ajouter un aliment / Scanner")
    }
    OutlinedButton(onClick = { vm.clearPhotoEstimate(); photoDialog = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.PhotoCamera, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Analyser mon repas en photo (IA)")
    }
    Panel("Journal alimentaire • aujourd'hui") {
        if (dayMeals.isEmpty()) Text("Aucun aliment ajouté pour aujourd'hui.")
        dayMeals.forEach { meal ->
            val n = meal.nutrition()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(meal.name, fontWeight = FontWeight.SemiBold)
                    Text("${meal.mealType} • ${fmt(meal.grams)} g • ${fmt(n.calories)} kcal", style = MaterialTheme.typography.bodySmall)
                    if (meal.source == "photo_ai_confirmed") {
                        Text("Estimation photo validée", style = MaterialTheme.typography.labelSmall, color = primary)
                    }
                    Text("P ${fmt(n.protein)} | G ${fmt(n.carbs)} | L ${fmt(n.fat)} g", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = { vm.deleteMeal(meal.id) }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer") }
            }
            HorizontalDivider()
        }
    }
    if (adding) FoodDialog(vm, food, loading, onClose = { adding = false })
    if (photoDialog) PhotoMealDialog(vm, onClose = { photoDialog = false; vm.clearPhotoEstimate() })
}

@Composable
private fun FoodDialog(vm: CoachViewModel, found: FoodLookup?, loading: Boolean, onClose: () -> Unit) {
    val context = LocalContext.current
    var barcode by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("100") }
    var kcal by remember { mutableStateOf("") }
    var prot by remember { mutableStateOf("") }
    var carb by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var mealType by remember { mutableStateOf("Déjeuner") }
    LaunchedEffect(found) {
        found?.let {
            barcode = it.barcode; name = it.name
            kcal = fmt(it.per100.calories); prot = fmt(it.per100.protein)
            carb = fmt(it.per100.carbs); fat = fmt(it.per100.fat)
        }
    }
    AlertDialog(onDismissRequest = onClose, title = { Text("Ajouter un aliment") },
        text = {
            Column(Modifier.heightIn(max = 510.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Code-barres ou saisie manuelle", style = MaterialTheme.typography.bodySmall)
                Field("Code-barres EAN / UPC", barcode, { barcode = it }, KeyboardType.Number)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.lookup(barcode) }, enabled = !loading, modifier = Modifier.weight(1f)) {
                        Text(if (loading) "Recherche…" else "Rechercher")
                    }
                    OutlinedButton(onClick = {
                        startScanner(context, onFound = { barcode = it; vm.lookup(it) }, onError = vm::inform)
                    }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.QrCodeScanner, contentDescription = null); Text(" Scan") }
                }
                Text("Valeurs nutritionnelles pour 100 g", style = MaterialTheme.typography.labelMedium)
                Field("Nom de l'aliment", name, { name = it })
                Field("Quantité consommée (g)", grams, { grams = it }, KeyboardType.Decimal)
                Field("Calories / 100 g", kcal, { kcal = it }, KeyboardType.Decimal)
                Field("Protéines / 100 g", prot, { prot = it }, KeyboardType.Decimal)
                Field("Glucides / 100 g", carb, { carb = it }, KeyboardType.Decimal)
                Field("Lipides / 100 g", fat, { fat = it }, KeyboardType.Decimal)
                Text("Type de repas : $mealType", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Petit-déjeuner", "Déjeuner", "Dîner", "Collation").forEach {
                        FilterChip(selected = mealType == it, onClick = { mealType = it }, label = { Text(it) })
                    }
                }
                Text("Données Open Food Facts : vérifier l'étiquette. Pour un plat non emballé, saisir les valeurs par 100 g.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                val g = number(grams); val k = number(kcal); val p = number(prot); val c = number(carb); val f = number(fat)
                if (g == null || k == null || p == null || c == null || f == null) vm.inform("Complétez toutes les valeurs numériques")
                else if (vm.addMeal(name, barcode, mealType, g, MacroValues(k,p,c,f))) onClose()
            }, colors = ButtonDefaults.buttonColors(containerColor = primary)) { Text("Ajouter") }
        }, dismissButton = { TextButton(onClick = onClose) { Text("Annuler") } }
    )
}

private fun startScanner(context: Context, onFound: (String) -> Unit, onError: (String) -> Unit) {
    try {
        GmsBarcodeScanning.getClient(context).startScan()
            .addOnSuccessListener { code -> code.rawValue?.let(onFound) ?: onError("Code illisible") }
            .addOnFailureListener { e -> onError("Scanner indisponible : ${e.localizedMessage ?: "réessayez ou saisissez le code"}") }
    } catch (ex: Exception) { onError("Service de scan indisponible. Utilisez la saisie manuelle.") }
}

@Composable
private fun SportScreen(vm: CoachViewModel, programs: List<Program>, exercises: List<Exercise>, logs: List<WorkoutLog>, coach: Boolean, today: String) {
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var addProgram by remember { mutableStateOf(false) }
    var addToProgram by remember { mutableStateOf<Long?>(null) }
    Text("Programmes sportifs", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    if (coach) Button(onClick = { addProgram = true }, colors = ButtonDefaults.buttonColors(containerColor = primary)) {
        Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(5.dp)); Text("Créer un programme")
    }
    if (programs.isEmpty()) Panel("Démarrer") { Text(if (coach) "Créez un programme puis ajoutez des exercices." else "Votre coach n'a pas encore créé de programme.") }
    programs.forEach { program ->
        val items = exercises.filter { it.programId == program.id }
        val doneCount = items.count { e -> logs.any { it.exerciseId == e.id && it.day == today } }
        Panel("${program.title} • ${program.dayLabel}") {
            Text("$doneCount / ${items.size} exercices enregistrés aujourd'hui", style = MaterialTheme.typography.bodySmall)
            items.forEach { ex ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(ex.name, fontWeight = FontWeight.SemiBold)
                        Text("${ex.sets} × ${ex.reps} • ${fmt(ex.targetLoadKg)} kg • ${ex.restSeconds} s repos", style = MaterialTheme.typography.bodySmall)
                    }
                    val already = logs.any { it.exerciseId == ex.id && it.day == today }
                    OutlinedButton(onClick = { selectedExercise = ex }) { Text(if (already) "Refaire" else "Noter") }
                }
                HorizontalDivider()
            }
            if (coach) {
                OutlinedButton(onClick = { addToProgram = program.id }) { Text("+ Ajouter un exercice") }
                TextButton(onClick = { vm.deleteProgram(program.id) }) { Text("Supprimer le programme") }
            }
        }
    }
    selectedExercise?.let { ex -> LogExerciseDialog(vm, ex, onClose = { selectedExercise = null }) }
    if (addProgram) ProgramDialog(vm, onClose = { addProgram = false })
    addToProgram?.let { id -> ExerciseDialog(vm, id, onClose = { addToProgram = null }) }
}

@Composable
private fun ProgramDialog(vm: CoachViewModel, onClose: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("Jour 1") }
    AlertDialog(onDismissRequest = onClose, title = { Text("Nouveau programme") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Field("Nom (ex : Force haut du corps)", title, { title = it })
            Field("Jour (ex : Lundi / Jour 1)", day, { day = it })
        }
    }, confirmButton = { Button(onClick = { if (vm.addProgram(title, day)) onClose() }) { Text("Créer") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Annuler") } })
}

@Composable
private fun ExerciseDialog(vm: CoachViewModel, programId: Long, onClose: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("4") }
    var reps by remember { mutableStateOf("8") }
    var load by remember { mutableStateOf("0") }
    var rest by remember { mutableStateOf("90") }
    var muscle by remember { mutableStateOf(Muscle.LEGS) }
    var manuallyPicked by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onClose, title = { Text("Ajouter un exercice") }, text = {
        Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Field("Exercice", name, { name = it; if (!manuallyPicked) muscle = detectMuscle(it) ?: muscle })
            Text("Groupe musculaire principal", style = MaterialTheme.typography.labelMedium)
            Muscle.entries.chunked(3).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    group.forEach { item -> FilterChip(selected = muscle == item,
                        onClick = { muscle = item; manuallyPicked = true }, label = { Text(item.label) }) }
                }
            }
            Field("Séries", sets, { sets = it }, KeyboardType.Number)
            Field("Répétitions", reps, { reps = it }, KeyboardType.Number)
            Field("Charge cible (kg)", load, { load = it }, KeyboardType.Decimal)
            Field("Repos (secondes)", rest, { rest = it }, KeyboardType.Number)
        }
    }, confirmButton = { Button(onClick = {
        val s = numberInt(sets); val r = numberInt(reps); val l = number(load); val t = numberInt(rest)
        if (s == null || r == null || l == null || t == null) vm.inform("Vérifiez les champs numériques")
        else if (vm.addExercise(programId, name, s, r, l, t, muscle)) onClose()
    }) { Text("Ajouter") } }, dismissButton = { TextButton(onClick = onClose) { Text("Annuler") } })
}

@Composable
private fun LogExerciseDialog(vm: CoachViewModel, exercise: Exercise, onClose: () -> Unit) {
    var sets by remember { mutableStateOf(exercise.sets.toString()) }
    var reps by remember { mutableStateOf(exercise.reps.toString()) }
    var load by remember { mutableStateOf(exercise.targetLoadKg.toString()) }
    var effort by remember { mutableStateOf("7") }
    AlertDialog(onDismissRequest = onClose, title = { Text("Performance • ${exercise.name}") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Field("Séries réalisées", sets, { sets = it }, KeyboardType.Number)
            Field("Répétitions par série", reps, { reps = it }, KeyboardType.Number)
            Field("Charge utilisée (kg)", load, { load = it }, KeyboardType.Decimal)
            Field("Difficulté ressentie (RPE 1 à 10)", effort, { effort = it }, KeyboardType.Number)
        }
    }, confirmButton = { Button(onClick = {
        val s = numberInt(sets); val r = numberInt(reps); val l = number(load); val e = numberInt(effort)
        if (s == null || r == null || l == null || e == null) vm.inform("Données d'entraînement incomplètes")
        else if (vm.addLog(exercise.id, s, r, l, e)) onClose()
    }) { Text("Enregistrer") } }, dismissButton = { TextButton(onClick = onClose) { Text("Annuler") } })
}

@Composable
private fun ProgressScreen(vm: CoachViewModel, metrics: List<BodyMetric>, logs: List<WorkoutLog>, meals: List<Meal>) {
    var addWeight by remember { mutableStateOf(false) }
    Text("Ma progression", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Button(onClick = { addWeight = true }, colors = ButtonDefaults.buttonColors(containerColor = primary)) {
        Icon(Icons.Default.MonitorWeight, contentDescription = null); Spacer(Modifier.width(7.dp)); Text("Ajouter une pesée")
    }
    Panel("Évolution corporelle") {
        if (metrics.isEmpty()) Text("Aucune donnée corporelle.")
        metrics.take(12).forEach { Text("${it.day} : ${fmt(it.weightKg)} kg") }
    }
    Panel("Journal sportif récent") {
        if (logs.isEmpty()) Text("Aucune performance enregistrée.")
        logs.take(12).forEach { Text("${it.day} : ${it.setsDone} × ${it.repsDone} à ${fmt(it.loadKg)} kg (RPE ${it.perceivedEffort})") }
    }
    Panel("Nutrition enregistrée") {
        Text("${meals.size} aliment(s) enregistrés au total")
        val activeDays = meals.map { it.day }.distinct().size
        Text("$activeDays jour(s) avec au moins un aliment saisi")
    }
    if (addWeight) WeightDialog(vm, onClose = { addWeight = false })
}

@Composable
private fun WeightDialog(vm: CoachViewModel, onClose: () -> Unit) {
    var weight by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onClose, title = { Text("Ajouter une pesée") },
        text = { Field("Poids (kg)", weight, { weight = it }, KeyboardType.Decimal) },
        confirmButton = { Button(onClick = { val w = number(weight); if (w == null) vm.inform("Poids manquant") else if (vm.addWeight(w)) onClose() }) { Text("Enregistrer") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Annuler") } })
}

@Composable
private fun CoachScreen(vm: CoachViewModel, goal: Goal, total: MacroValues, dayLogs: List<WorkoutLog>, metrics: List<BodyMetric>,
                        programs: List<Program>, exercises: List<Exercise>, dayMeals: List<Meal>) {
    var kcal by remember(goal) { mutableStateOf(goal.calories.toString()) }
    var protein by remember(goal) { mutableStateOf(goal.protein.toString()) }
    var carbs by remember(goal) { mutableStateOf(goal.carbs.toString()) }
    var fat by remember(goal) { mutableStateOf(goal.fat.toString()) }
    var weight by remember { mutableStateOf(metrics.firstOrNull()?.weightKg?.toString() ?: "") }
    var pKg by remember { mutableStateOf("1.8") }
    var fKg by remember { mutableStateOf("0.9") }
    Text("Espace coach", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text("Dossier démo : Athlète 1", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Panel("Synthèse du suivi") {
        Text("Repas aujourd'hui : ${dayMeals.size} • ${fmt(total.calories)} kcal")
        Text("Exercices notés aujourd'hui : ${dayLogs.size}")
        Text("Programmes : ${programs.size} • Exercices prescrits : ${exercises.size}")
        Text("Dernière pesée : ${metrics.firstOrNull()?.let { "${fmt(it.weightKg)} kg" } ?: "non renseignée"}")
    }
    Panel("Objectifs nutritionnels de l'athlète") {
        Text("Les valeurs doivent être établies et validées par le coach.", style = MaterialTheme.typography.bodySmall)
        Field("Calories cibles", kcal, { kcal = it }, KeyboardType.Number)
        Field("Protéines (g)", protein, { protein = it }, KeyboardType.Number)
        Field("Glucides (g)", carbs, { carbs = it }, KeyboardType.Number)
        Field("Lipides (g)", fat, { fat = it }, KeyboardType.Number)
        Button(onClick = {
            val k = numberInt(kcal); val p = numberInt(protein); val c = numberInt(carbs); val f = numberInt(fat)
            if (k == null || p == null || c == null || f == null) vm.inform("Objectifs incomplets")
            else vm.saveGoal(MacroTarget(k, p, c, f))
        }, colors = ButtonDefaults.buttonColors(containerColor = primary)) { Text("Enregistrer les objectifs") }
        HorizontalDivider()
        Text("Calculatrice de répartition des macros", fontWeight = FontWeight.Bold)
        Field("Poids de référence (kg)", weight, { weight = it }, KeyboardType.Decimal)
        Field("Protéines choisies (g/kg)", pKg, { pKg = it }, KeyboardType.Decimal)
        Field("Lipides choisis (g/kg)", fKg, { fKg = it }, KeyboardType.Decimal)
        OutlinedButton(onClick = {
            val k = numberInt(kcal); val w = number(weight); val p = number(pKg); val f = number(fKg)
            if (k == null || w == null || p == null || f == null) vm.inform("Paramètres manquants")
            else runCatching { targetFromCalories(k, w, p, f) }
                .onSuccess { protein = it.protein.toString(); carbs = it.carbs.toString(); fat = it.fat.toString() }
                .onFailure { vm.inform(it.message ?: "Calcul impossible") }
        }) { Text("Calculer la répartition") }
        Text("Calcul : P × 4 + G × 4 + L × 9 ≈ kcal. Le calcul ne définit pas à lui seul les besoins énergétiques.",
            style = MaterialTheme.typography.bodySmall)
    }
    Panel("Coordination coach / athlète") {
        Text("Les repas, les pesées et les performances saisis dans les autres onglets alimentent automatiquement cette synthèse.")
        Text("Le mode coach est une simulation locale. Authentification et permissions serveur seront nécessaires avant tout usage multi-athlètes.",
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Field(label: String, value: String, change: (String) -> Unit, keyboard: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(value = value, onValueChange = change, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard), modifier = Modifier.fillMaxWidth())
}
