package com.coachlink.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coachlink.app.ui.CoachLinkApp
import com.coachlink.app.ui.CoachViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val colors = darkColorScheme(
                primary = Color(0xFF2CF2B0),
                onPrimary = Color(0xFF061B16),
                secondary = Color(0xFFFF8659),
                onSecondary = Color(0xFF231109),
                tertiary = Color(0xFF66D5FA),
                background = Color(0xFF080F1A),
                onBackground = Color(0xFFF2F7F8),
                surface = Color(0xFF111C29),
                onSurface = Color(0xFFF3F7FA),
                surfaceVariant = Color(0xFF233243),
                onSurfaceVariant = Color(0xFFB5C6D1),
                surfaceContainerLow = Color(0xFF11202D),
                surfaceContainerHigh = Color(0xFF182A39),
                outline = Color(0xFF5C7381),
                outlineVariant = Color(0xFF304555),
                error = Color(0xFFFF8575)
            )
            MaterialTheme(colorScheme = colors) { CoachLinkApp(viewModel<CoachViewModel>()) }
        }
    }
}
