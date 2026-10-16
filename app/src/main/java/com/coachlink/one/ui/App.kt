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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coachlink.one.data.*

@Composable fun TrackerApp(vm:TrackerViewModel) {
    val ctx=LocalContext.current
    val lock=remember {LocalLock(ctx)}
    var unlocked by remember {mutableStateOf(false)}
    var configured by remember {mutableStateOf(lock.configured)}
    val profile by vm.profile.collectAsStateWithLifecycle(initialValue=null)
    val measurements by vm.measurements.collectAsStateWithLifecycle(initialValue=emptyList())
    val foods by vm.foods.collectAsStateWithLifecycle(initialValue=emptyList())
    val programs by vm.programs.collectAsStateWithLifecycle(initialValue=emptyList())
    val exercises by vm.exercises.collectAsStateWithLifecycle(initialValue=emptyList())
    val logs by vm.logs.collectAsStateWithLifecycle(initialValue=emptyList())
    val friends by vm.friends.collectAsStateWithLifecycle(initialValue=emptyList())
    val message by vm.notice.collectAsStateWithLifecycle(initialValue="")
    val snack=remember {SnackbarHostState()}
    LaunchedEffect(message) { if(message.isNotBlank()) {snack.showSnackbar(message);vm.clearNotice()} }
    if(!unlocked) {
        LoginScreen(configured,lock,onCreated={name->
            vm.saveProfile(Profile(name=name,age=0,sex="NA",heightCm=0.0,startWeightKg=0.0,goalWeightKg=0.0,goalDate="",objective="MAINTAIN"))
            configured=true;unlocked=true
        },onUnlocked={unlocked=true})
        return
    }
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
                IconButton(onClick={unlocked=false}) {Icon(Icons.Default.LockOutline,"Verrouiller")}
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
                0 -> ProfileScreen(vm,profile,measurements,foods,logs,friends)
                1 -> MeasuresScreen(vm,profile,measurements)
                2 -> NutritionScreen(vm,profile,measurements,foods)
                else -> SportScreen(vm,programs,exercises,logs)
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
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),contentAlignment=Alignment.Center) {
        Column(Modifier.padding(26.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Icon(Icons.Default.Bolt,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(52.dp))
            Text("COACHLINK",fontWeight=FontWeight.Black,style=MaterialTheme.typography.displaySmall)
            Text("ONE / PERSONAL TRACKER",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
            Text(if(configured) "Accède à ton espace personnel" else "Crée ton espace de suivi local",style=MaterialTheme.typography.titleMedium)
            if(!configured) Field("Ton prénom",name,{name=it})
            OutlinedTextField(value=pin,onValueChange={pin=it.filter(Char::isDigit).take(6)},label={Text("PIN à 6 chiffres")},
                modifier=Modifier.fillMaxWidth(),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),
                visualTransformation=PasswordVisualTransformation())
            if(!configured) OutlinedTextField(value=confirm,onValueChange={confirm=it.filter(Char::isDigit).take(6)},
                label={Text("Confirme le PIN")},modifier=Modifier.fillMaxWidth(),singleLine=true,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),visualTransformation=PasswordVisualTransformation())
            if(error.isNotEmpty()) Text(error,color=MaterialTheme.colorScheme.error)
            Button(onClick={
                if(configured) {if(lock.check(pin)) {error="";onUnlocked()} else error="PIN incorrect ou verrouillage temporaire"}
                else if(name.trim().length<2 || pin.length!=6 || pin!=confirm) error="Prénom et codes PIN identiques requis"
                else if(lock.create(pin)) onCreated(name.trim()) else error="Impossible de créer ce code"
            },modifier=Modifier.fillMaxWidth().height(55.dp)) {Text(if(configured)"Se connecter" else "Créer mon suivi",fontWeight=FontWeight.Bold)}
            Text("Connexion locale par PIN : pas de compte cloud ni de récupération à distance dans cette version.",
                color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
        }
    }
}
