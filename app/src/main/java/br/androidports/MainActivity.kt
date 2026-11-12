package br.androidports

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.androidports.data.local.AppSettings
import br.androidports.ui.nav.PortsApp
import br.androidports.ui.theme.PortsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PortsApplication
        setContent {
            val settings by app.container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            PortsTheme(
                themeMode = settings.themeMode,
                haptics = settings.haptics,
                reduceMotion = settings.reduceMotion,
            ) {
                PortsApp(container = app.container)
            }
        }
    }
}
