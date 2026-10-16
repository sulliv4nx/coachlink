package com.coachlink.one

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coachlink.one.ui.TrackerApp
import com.coachlink.one.ui.TrackerViewModel

private val palette = darkColorScheme(
    primary = Color(0xFFCBF36D), onPrimary = Color(0xFF16270C),
    secondary = Color(0xFF76B8FF), onSecondary = Color(0xFF101E30),
    tertiary = Color(0xFFB9A1FF), onTertiary = Color(0xFF201433),
    background = Color(0xFF0B1423), onBackground = Color(0xFFF7FAFE),
    surface = Color(0xFF111F30), onSurface = Color(0xFFF7FAFE),
    surfaceVariant = Color(0xFF283849), onSurfaceVariant = Color(0xFFC4D1DE),
    surfaceContainerLow = Color(0xFF142435), surfaceContainerHigh = Color(0xFF213447),
    outline = Color(0xFF6D8499), outlineVariant = Color(0xFF3D5269),
    error = Color(0xFFFF917F)
)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(11,20,35)
        window.navigationBarColor = android.graphics.Color.rgb(11,20,35)
        setContent { MaterialTheme(colorScheme = palette) { TrackerApp(viewModel<TrackerViewModel>()) } }
    }
}
