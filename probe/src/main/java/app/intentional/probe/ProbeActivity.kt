package app.intentional.probe

import android.app.Activity
import android.os.Bundle
import android.widget.*

/** A local, network-free surface for foreground and keyboard integration checks. */
class ProbeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val padding = (24 * resources.displayMetrics.density).toInt()
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding * 3, padding, padding)
            addView(TextView(context).apply { text = getString(app.intentional.probe.R.string.app_name); textSize = 28f })
            addView(TextView(context).apply { text = "Local verification surface. No accounts or network access."; textSize = 16f })
            addView(EditText(context).apply { hint = "Keyboard check"; setSingleLine(true) })
            addView(Button(context).apply { text = "Show temporary dialog"; setOnClickListener {
                android.app.AlertDialog.Builder(context).setTitle("Temporary interruption").setMessage("Return to the same session.").setPositiveButton("Done", null).show()
            } })
        })
    }
}
