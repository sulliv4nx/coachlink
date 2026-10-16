package com.coachlink.one.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coachlink.one.data.*

@Composable fun TrackerApp(vm:TrackerViewModel) {
    val ctx=LocalContext.current
    val lock=remember {LocalLock(ctx)}
    var unlocked by remember {mutableStateOf(false)}
    var configured by remember {mutableStateOf(lock.configured)}
    if(!unlocked) {
        LoginScreen(configured,lock,onCreated={name->
            // New profile creation is attempted only after the PIN has been persisted.
            vm.saveProfile(Profile(name=name,age=0,sex="NA",heightCm=0.0,startWeightKg=0.0,goalWeightKg=0.0,goalDate="",objective="MAINTAIN"))
            configured=true
            unlocked=true
        },onUnlocked={unlocked=true})
        return
    }
    val profile by vm.profile.collectAsStateWithLifecycle(initialValue=null)
    val measurements by vm.measurements.collectAsStateWithLifecycle(initialValue=emptyList())
    val foods by vm.foods.collectAsStateWithLifecycle(initialValue=emptyList())
    val programs by vm.programs.collectAsStateWithLifecycle(initialValue=emptyList())
    val exercises by vm.exercises.collectAsStateWithLifecycle(initialValue=emptyList())
    val logs by vm.logs.collectAsStateWithLifecycle(initialValue=emptyList())
    val friends by vm.friends.collectAsStateWithLifecycle(initialValue=emptyList())
    val sportGoals by vm.sportGoals.collectAsStateWithLifecycle(initialValue=emptyList())
    val sportSessions by vm.sportSessions.collectAsStateWithLifecycle(initialValue=emptyList())
    val workoutPlans by vm.workoutPlans.collectAsStateWithLifecycle(initialValue=emptyList())
    val workoutBlocks by vm.workoutBlocks.collectAsStateWithLifecycle(initialValue=emptyList())
    val workoutMovements by vm.workoutMovements.collectAsStateWithLifecycle(initialValue=emptyList())
    val workoutRuns by vm.workoutRuns.collectAsStateWithLifecycle(initialValue=emptyList())
    val workoutSetResults by vm.workoutSetResults.collectAsStateWithLifecycle(initialValue=emptyList())
    val workoutBlockResults by vm.workoutBlockResults.collectAsStateWithLifecycle(initialValue=emptyList())
    val message by vm.notice.collectAsStateWithLifecycle(initialValue="")
    val snack=remember {SnackbarHostState()}
    LaunchedEffect(message) { if(message.isNotBlank()) {snack.showSnackbar(message);vm.clearNotice()} }
    var tab by remember {mutableIntStateOf(0)}
    val labels=listOf("Profil","Mesures","Nutrition","Sport")
    val icons=listOf(Icons.Default.Dashboard,Icons.Default.Straighten,Icons.Default.Restaurant,Icons.Default.FitnessCenter)
    Scaffold(containerColor=MaterialTheme.colorScheme.background,
        snackbarHost={SnackbarHost(snack)},
        topBar={Surface(color=Color(0xFF122337)) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.SpaceBetween) {
                Column {Text("COACHLINK / ONE",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Black,
                    style=MaterialTheme.typography.titleLarge)
                    Text("TON SUIVI • TON RYTHME",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.labelSmall)}
                IconButton(onClick={unlocked=false}) {Icon(Icons.Default.Lock,"Verrouiller")}
            }
        }},
        bottomBar={NavigationBar(containerColor=Color(0xFF122337)) {
            labels.forEachIndexed{i,label->NavigationBarItem(selected=tab==i,onClick={tab=i},
                icon={Icon(icons[i],label)},label={Text(label)})}
        }}
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement=Arrangement.spacedBy(14.dp)) {
            when(tab) {
                0 -> ProfileScreen(vm,profile,measurements,foods,logs,friends,sportGoals,sportSessions)
                1 -> MeasuresScreen(vm,profile,measurements)
                2 -> NutritionScreen(vm,profile,measurements,foods)
                else -> SportScreen(vm,programs,exercises,logs,sportGoals,sportSessions,measurements.firstOrNull()?.weightKg?:profile?.startWeightKg,
                    workoutPlans,workoutBlocks,workoutMovements,workoutRuns,workoutSetResults,workoutBlockResults)
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable private fun LoginScreen(configured:Boolean,lock:LocalLock,onCreated:(String)->Unit,onUnlocked:()->Unit) {
    var name by remember {mutableStateOf("")}
    var pin by remember {mutableStateOf("")}
    var confirm by remember {mutableStateOf("")}
    var error by remember {mutableStateOf("")}
    var busy by remember {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    val focus=LocalFocusManager.current
    fun submit() {
        if(busy)return
        if(configured && pin.length!=6) {error="Saisis ton PIN à 6 chiffres.";return}
        if(!configured && (name.trim().length<2 || pin.length!=6 || pin!=confirm)) {
            error="Indique ton prénom et deux codes PIN identiques de 6 chiffres."
            return
        }
        focus.clearFocus()
        error=""
        busy=true
        // PBKDF2 can be costly on certain devices: never block the UI thread.
        scope.launch {
            val result=runCatching {
                withContext(Dispatchers.Default) {
                    if(configured)lock.check(pin) else lock.create(pin)
                }
            }
            busy=false
            result.onSuccess { accepted ->
                if(accepted) {
                    if(configured)onUnlocked() else onCreated(name.trim())
                } else {
                    error=if(configured) "PIN incorrect, ou attente temporaire après plusieurs essais." else
                        "Le PIN n'a pas pu être enregistré. Réessaie."
                }
            }.onFailure {
                error="Erreur lors de la connexion : ${it.message ?: "vérification impossible"}"
            }
        }
    }
    Box(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment=Alignment.Center
    ) {
        Column(Modifier.fillMaxWidth().widthIn(max=440.dp).padding(24.dp),
            verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.Bolt,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(48.dp))
            Text("COACHLINK",fontWeight=FontWeight.Black,style=MaterialTheme.typography.displaySmall)
            Text("ONE / PERSONAL TRACKER",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
            Text(if(configured) "Connecte-toi à ton espace" else "Crée ton espace personnel",
                style=MaterialTheme.typography.titleMedium)
            if(!configured) Field("Ton prénom",name,{name=it})
            OutlinedTextField(
                value=pin,
                onValueChange={pin=it.filter(Char::isDigit).take(6);error=""},
                label={Text("PIN à 6 chiffres")},
                modifier=Modifier.fillMaxWidth(),singleLine=true,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword,
                    imeAction=if(configured)ImeAction.Done else ImeAction.Next),
                keyboardActions=KeyboardActions(onDone={submit()}),
                visualTransformation=PasswordVisualTransformation()
            )
            if(!configured) OutlinedTextField(
                value=confirm,onValueChange={confirm=it.filter(Char::isDigit).take(6);error=""},
                label={Text("Confirmer le PIN")},
                modifier=Modifier.fillMaxWidth(),singleLine=true,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword,imeAction=ImeAction.Done),
                keyboardActions=KeyboardActions(onDone={submit()}),
                visualTransformation=PasswordVisualTransformation()
            )
            if(error.isNotEmpty())Text(error,color=MaterialTheme.colorScheme.error)
            Button(onClick={submit()},enabled=!busy,modifier=Modifier.fillMaxWidth().height(56.dp)) {
                if(busy) {
                    CircularProgressIndicator(modifier=Modifier.size(20.dp),strokeWidth=2.dp,
                        color=MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(10.dp))
                }
                Text(if(busy)"Vérification…" else if(configured)"Se connecter" else "Créer mon suivi",
                    fontWeight=FontWeight.Bold)
            }
            Text("Le bouton reste accessible même si le clavier Android est ouvert.",
                color=MaterialTheme.colorScheme.onSurfaceVariant,
                style=MaterialTheme.typography.labelSmall)
            Text("Code PIN local : pas de compte cloud ni de récupération à distance.",
                color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
        }
    }
}
