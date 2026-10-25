package app.intentional.pause

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.intentional.pause.ui.IntentRoot
import app.intentional.pause.ui.IntentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { IntentTheme { IntentRoot((application as IntentApp).store) } }
    }
}
