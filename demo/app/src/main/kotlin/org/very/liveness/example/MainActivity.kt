package org.very.liveness.example

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.very.liveness.VeryAILiveness
import org.very.liveness.VeryLivenessConfig

/**
 * Minimal demo of the standalone `VeryAILiveness` SDK
 * (`org.very:liveness` artifact). Notice that nothing in this file
 * imports `org.very.sdk.VerySDK` or any auth-flow class — the liveness
 * artifact's compile classpath does not expose them.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var resultLabel: TextView
    private lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        resultLabel = findViewById(R.id.resultLabel)
        startButton = findViewById(R.id.startLivenessButton)

        startButton.setOnClickListener { runLivenessCheck() }
    }

    private fun runLivenessCheck() {
        startButton.isEnabled = false
        statusText.text = "Starting liveness check..."

        val config = VeryLivenessConfig(
            sdkKey = "veryai_sdk_EvY1fzQDal-3XLkhyegxUg6Vr03wVzmmmHLCGkp5EnM",  // staging SDK key — replace with yours
            themeMode = "light",
            language = "en",
        )

        config.showError = true

        VeryAILiveness.check(
            context = this,
            config = config,
        ) { result ->
            runOnUiThread {
                startButton.isEnabled = true
                val details = buildString {
                    appendLine("── Liveness Result ──")
                    appendLine("code: ${result.code}")
                    appendLine("error: ${result.error ?: "(null)"}")
                    appendLine("errorMessage: ${result.errorMessage ?: "(null)"}")
                    appendLine("isSuccess: ${result.isSuccess}")
                }
                when {
                    result.isSuccess -> {
                        statusText.text = "Liveness check passed!"
                        statusText.setTextColor(Color.parseColor("#2E7D32"))
                    }
                    result.code == "cancelled" -> {
                        statusText.text = "Liveness check cancelled by user"
                        statusText.setTextColor(Color.parseColor("#E65100"))
                    }
                    else -> {
                        statusText.text = "Liveness check failed"
                        statusText.setTextColor(Color.parseColor("#C62828"))
                    }
                }
                resultLabel.text = details
            }
        }
    }
}
