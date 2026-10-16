package com.coachlink.one.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.util.Locale

val today: String get() = LocalDate.now().toString()
fun fmt(n: Double, places: Int=1): String = String.format(Locale.FRANCE,"%.${places}f",n)
fun double(s: String) = s.trim().replace(',','.').toDoubleOrNull()
fun validDate(s: String) = runCatching { LocalDate.parse(s) }.isSuccess
fun objectiveLabel(s: String) = when(s) {"LOSS"->"Perte de poids";"GAIN"->"Prise de masse";else->"Maintien / performance"}
val mealKinds = listOf("Petit-déjeuner", "Déjeuner", "Dîner", "Encas", "Shaker")

@Composable fun Panel(title:String,modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
    ElevatedCard(modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),
        colors=CardDefaults.elevatedCardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(17.dp),verticalArrangement=Arrangement.spacedBy(11.dp)) {
            Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.ExtraBold)
            content()
        }
    }
}
@Composable fun Field(label:String,value:String,onChange:(String)->Unit,decimal:Boolean=false) {
    OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=true,
        keyboardOptions=KeyboardOptions(keyboardType=if(decimal) KeyboardType.Decimal else KeyboardType.Text))
}
@Composable fun Metric(label:String,value:String,color:Color,modifier:Modifier=Modifier) {
    Surface(modifier,shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(label,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.labelMedium)
            Text(value,color=color,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge)
        }
    }
}
