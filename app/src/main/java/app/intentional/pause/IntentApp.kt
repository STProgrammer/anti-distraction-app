package app.intentional.pause

import android.app.Application
import app.intentional.pause.data.Store

class IntentApp : Application() {
    val store: Store by lazy { Store(this) }
}
