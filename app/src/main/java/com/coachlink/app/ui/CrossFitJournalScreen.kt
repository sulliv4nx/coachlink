package com.coachlink.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.coachlink.app.data.CrossFitSession
import com.coachlink.app.domain.Muscle
import com.coachlink.app.domain.detectWodMuscles
import java.time.LocalDate

/** Same local journal is visible to the demo coach and used by the adaptive engine. */
@Composable
fun CrossFitJournalScreen(vm: CoachViewModel, sessions: List<CrossFitSession>) {
    var showForm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Long?>(null) }
    val today = LocalDate.now()
    val weekCount = sessions.count {
        runCatching {
            val day = LocalDate.parse(it.day)
            !day.isAfter(today) && !day.isBefore(today.minusDays(6))
        }.getOrDefault(false)
    }
    ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("CROSSFIT • JOURNAL DE WOD", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Text(weekCount.toString() + " cours sur les 7 derniers jours",
                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text("Note ton cours réel, même s'il ne vient pas de CoachLink. Le coach utilise les muscles, l'intensité et la durée pour adapter la prochaine séance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { showForm = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ajouter mon cours CrossFit")
            }
            if (sessions.isEmpty()) {
                Text("Aucun WOD enregistré. Ajoute le cours d'aujourd'hui.",
                    style = MaterialTheme.typography.bodySmall)
            }
            sessions.take(8).forEach { item ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.title + "  •  " + item.format, fontWeight = FontWeight.Bold)
                    Text(item.day + "  •  " + item.durationMinutes + " min  •  RPE " + item.rpe,
                        style = MaterialTheme.typography.labelMedium)
                    Text(item.wod, style = MaterialTheme.typography.bodySmall, maxLines = 4)
                    Text(item.muscleGroups.split(",").mapNotNull { label ->
                        Muscle.entries.find { it.name == label }?.label
                    }.joinToString(" · "),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium)
                    if (item.pain) Text("Douleur signalée : prudence pour la prochaine séance.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { pendingDelete = item.id }) { Text("Retirer ce cours") }
                }
            }
        }
    }
    if (showForm) CrossFitLogDialog(vm) { showForm = false }
    pendingDelete?.let { id ->
        AlertDialog(onDismissRequest = { pendingDelete = null },
            title = { Text("Supprimer ce WOD ?") },
            text = { Text("Il ne sera plus pris en compte par le coach adaptatif.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteCrossfit(id); pendingDelete = null }) {
                    Text("Supprimer")
                }
            }, dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Annuler") }
            })
    }
}

@Composable
private fun CrossFitLogDialog(vm: CoachViewModel, onClose: () -> Unit) {
    var day by remember { mutableStateOf(LocalDate.now().toString()) }
    var title by remember { mutableStateOf("Cours CrossFit") }
    var format by remember { mutableStateOf("WOD") }
    var wod by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("45") }
    var rpe by remember { mutableFloatStateOf(7f) }
    var selectedMuscles by remember { mutableStateOf(setOf<Muscle>()) }
    var userEdited by remember { mutableStateOf(false) }
    var pain by remember { mutableStateOf(false) }
    LaunchedEffect(wod) {
        if (!userEdited) selectedMuscles = detectWodMuscles(wod)
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Enregistrer un cours CrossFit") },
        text = {
            Column(Modifier.heightIn(max = 525.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Nom du cours / WOD") }, singleLine = true)
                OutlinedTextField(value = day, onValueChange = { day = it },
                    label = { Text("Date (AAAA-MM-JJ)") }, singleLine = true)
                Text("Format", fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("WOD", "AMRAP", "EMOM", "For time", "Force", "Skill").forEach { option ->
                        FilterChip(selected = format == option, onClick = { format = option },
                            label = { Text(option) })
                    }
                }
                OutlinedTextField(value = wod, onValueChange = { wod = it },
                    label = { Text("Mouvements et contenu du cours") },
                    placeholder = { Text("Ex. AMRAP 15 min : 12 thrusters, 10 pull-ups, 200 m run") },
                    minLines = 3, maxLines = 6)
                OutlinedTextField(value = duration, onValueChange = { duration = it },
                    label = { Text("Durée totale du cours (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Text("Difficulté ressentie : RPE " + rpe.toInt() + "/10",
                    fontWeight = FontWeight.SemiBold)
                Slider(value = rpe, onValueChange = { rpe = it },
                    valueRange = 1f..10f, steps = 8)
                Text("Muscles travaillés : confirme la sélection", fontWeight = FontWeight.SemiBold)
                Text("Détection indicative selon les mouvements saisis. Coche aussi les autres zones sollicitées.",
                    style = MaterialTheme.typography.bodySmall)
                Muscle.entries.chunked(3).forEach { row ->
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        row.forEach { group ->
                            FilterChip(selected = group in selectedMuscles, onClick = {
                                userEdited = true
                                selectedMuscles = if (group in selectedMuscles) selectedMuscles - group
                                    else selectedMuscles + group
                            }, label = { Text(group.label) })
                        }
                    }
                }
                Row {
                    Checkbox(checked = pain, onCheckedChange = { pain = it })
                    Text("Douleur inhabituelle pendant le WOD", modifier = Modifier.padding(top = 13.dp),
                        style = MaterialTheme.typography.bodySmall)
                }
                Text("La charge estimée du WOD est un indicateur de fatigue, pas un nombre réel de séries.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (vm.saveCrossfit(day, title, format, wod, duration.toIntOrNull() ?: -1,
                        rpe.toInt(), selectedMuscles, pain)) onClose()
            }) { Text("Enregistrer le WOD") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Annuler") } }
    )
}
