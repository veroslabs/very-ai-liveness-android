package org.very.liveness.example

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
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
    private lateinit var showErrorSwitch: SwitchCompat
    private lateinit var showSuccessSwitch: SwitchCompat
    private lateinit var privacyMessageInput: EditText
    private lateinit var fontSizeInput: EditText
    private lateinit var scanTimeoutInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        resultLabel = findViewById(R.id.resultLabel)
        startButton = findViewById(R.id.startLivenessButton)
        showErrorSwitch = findViewById(R.id.showErrorSwitch)
        showSuccessSwitch = findViewById(R.id.showSuccessSwitch)
        privacyMessageInput = findViewById(R.id.privacyMessageInput)
        fontSizeInput = findViewById(R.id.fontSizeInput)
        scanTimeoutInput = findViewById(R.id.scanTimeoutInput)

        // Prefill with X's disclosure copy so the field is populated on launch;
        // edit either box to try your own copy / size.
        privacyMessageInput.setText(DEFAULT_PRIVACY_MESSAGE)
        fontSizeInput.setText("12")

        startButton.setOnClickListener { runLivenessCheck() }
    }

    /** Dismiss the soft keyboard when tapping outside the focused input. */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val focused = currentFocus
            if (focused is EditText) {
                val rect = Rect()
                focused.getGlobalVisibleRect(rect)
                if (!rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    focused.clearFocus()
                    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(focused.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun runLivenessCheck() {
        startButton.isEnabled = false
        statusText.text = "Starting liveness check..."

        val config = VeryLivenessConfig(
            sdkKey = "veryai_sdk_EvY1fzQDal-3XLkhyegxUg6Vr03wVzmmmHLCGkp5EnM",  // staging SDK key — replace with yours
            themeMode = "light",
            language = "en",
        )

        // When true, terminal errors route to an in-SDK error page with
        // Retry / Close instead of returning straight to this callback.
        config.showError = showErrorSwitch.isChecked
        // When true (default), the "Thanks for verifying" success page plays
        // before returning. Set false to return the instant capture succeeds.
        config.showSuccess = showSuccessSwitch.isChecked
        // Persist SDK logs to verysdk.log so the Appium e2e harness can adb
        // pull them on failure (file logging is gated on debugLogging).
        config.debugLogging = true
        // Host-supplied disclosure copy from the input box. Basic HTML is
        // honoured: an <a href> renders as an inline link the SDK opens on tap.
        config.privacyMessage = privacyMessageInput.text.toString().ifBlank { null }
        // Optional disclosure font size (sp) from the input box; blank/invalid
        // → 0, which uses the SDK default (12sp).
        config.privacyMessageFontSize = fontSizeInput.text.toString().trim().toFloatOrNull() ?: 0f
        // Optional scan-gesture timeout (seconds) from the input box; blank/invalid
        // → 0, which uses the SDK default (10s). Raise it for slower testing.
        config.scanTimeoutSeconds = scanTimeoutInput.text.toString().trim().toIntOrNull() ?: 0

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

    private companion object {
        // Demonstrates the supported inline HTML: <b> for bold, <font color> for
        // colour, and <a href> for a tappable link.
        const val DEFAULT_PRIVACY_MESSAGE =
            "<b>The app will briefly capture your hand motion solely to confirm you're a real " +
            "person and not a bot.</b><br><br>Your hand motion is " +
            "<font color=\"#68C906\">never recorded as a video</font> and " +
            "it never leaves your device. It is not used for any other purpose other than " +
            "anti-bot verification and will not be sold, leased, or traded. " +
            "<a href=\"https://help.x.com/en/rules-and-policies/real-person\">Learn more</a>"
    }
}
