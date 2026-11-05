package org.very.liveness.example

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import org.very.liveness.VeryAILiveness
import org.very.liveness.VeryLivenessConfig
import org.very.liveness.VeryLivenessSessionProvider
import org.very.sdk.VeryCustomString

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
    private lateinit var showYourHandInput: EditText
    private lateinit var connectDotsInput: EditText
    private lateinit var brokerUrlInput: EditText
    private lateinit var themeSpinner: Spinner

    private val themeOptions = arrayOf("dark", "light")

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
        showYourHandInput = findViewById(R.id.showYourHandInput)
        connectDotsInput = findViewById(R.id.connectDotsInput)
        brokerUrlInput = findViewById(R.id.brokerUrlInput)
        brokerUrlInput.hint = DEFAULT_BROKER_URL
        brokerUrlInput.setText(getPreferences(MODE_PRIVATE).getString(BROKER_URL_KEY, ""))
        themeSpinner = findViewById(R.id.themeSpinner)
        themeSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, themeOptions)

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
        // Liveness is decided by Very's server only (as in the web SDK), so a
        // check cannot start without the broker that issues its session.
        // An empty field uses the hosted demo broker.
        val typedUrl = brokerUrlInput.text.toString().trim()
        val brokerUrl = typedUrl.ifEmpty { DEFAULT_BROKER_URL }
        val brokerUri = android.net.Uri.parse(brokerUrl)
        if (brokerUri.scheme !in setOf("http", "https") || brokerUri.host.isNullOrEmpty()) {
            statusText.text = "Invalid session broker URL (leave it empty for the default)"
            statusText.setTextColor(Color.parseColor("#C62828"))
            return
        }
        // Only a typed URL is remembered; clearing the field goes back to the default.
        getPreferences(MODE_PRIVATE).edit().apply {
            if (typedUrl.isEmpty()) remove(BROKER_URL_KEY) else putString(BROKER_URL_KEY, typedUrl)
        }.apply()
        startButton.isEnabled = false
        statusText.text = "Starting liveness check..."

        val config = VeryLivenessConfig(
            sdkKey = "veryai_sdk_EvY1fzQDal-3XLkhyegxUg6Vr03wVzmmmHLCGkp5EnM",  // production SDK key — replace with yours
            themeMode = themeOptions[themeSpinner.selectedItemPosition],
            language = "en",
        )

        // When true, terminal errors park on an in-SDK Retry / Close sheet
        // over the camera instead of returning straight to this callback.
        config.showError = showErrorSwitch.isChecked
        // When true (default), the "Thanks for confirming" sheet shows and waits
        // for Close before returning. Set false to return the instant capture succeeds.
        config.showSuccess = showSuccessSwitch.isChecked
        // Persist SDK logs to verysdk.log so a failed run can be inspected with
        // adb pull (file logging is gated on debugLogging).
        config.debugLogging = true
        // The privacy notice and scan instruction are the app's saved text: they
        // ride on the session (see withSavedText below), the way a backend sends
        // the Developer Portal's text. config.privacyMessage and
        // customStrings.showYourHand would only stand in for a session without it.
        val savedNotice = privacyMessageInput.text.toString().trim()
        val savedInstruction = showYourHandInput.text.toString().trim()
        // Optional disclosure font size (sp) from the input box; blank/invalid
        // → 0, which uses the SDK default (12sp). It styles whichever notice shows.
        config.privacyMessageFontSize = fontSizeInput.text.toString().trim().toFloatOrNull() ?: 0f
        // Host-supplied override for the Touch prompt. Blank leaves it out of the
        // map entirely so the SDK's localized default wins.
        config.customStrings = connectDotsInput.text.toString().trim()
            .takeIf { it.isNotEmpty() }?.let { mapOf(VeryCustomString.CONNECT_DOTS to it) }
        // The SDK asks this app for an evidence session, which the broker
        // (the stand-in for this app's backend)
        // creates on Very production. The SDK then talks to production for the
        // check itself, so any API override is cleared.
        System.clearProperty("org.very.sdk.apiBase")
        config.multiPointTouch = true
        config.sessionProvider = VeryLivenessSessionProvider { request, callback ->
            Thread {
                try {
                    val conn = (java.net.URL(brokerUrl).openConnection() as java.net.HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        outputStream.use { it.write(request.toJson().toByteArray()) }
                    }
                    val ok = conn.responseCode in 200..299
                    val body = (if (ok) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
                    if (ok) callback.onSession(withSavedText(body, savedInstruction, savedNotice))
                    else callback.onError(IllegalStateException("broker HTTP ${conn.responseCode}: $body"))
                } catch (t: Throwable) {
                    callback.onError(t)
                }
            }.start()
        }

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
                    result.signedToken?.let { appendLine("signedToken: ${it.take(48)}…") }
                }
                when {
                    result.isSuccess -> {
                        statusText.text = "Liveness check passed!"
                        statusText.setTextColor(Color.parseColor("#2E7D32"))
                    }
                    result.code == "rejected" -> {
                        statusText.text = "Liveness check rejected by Very (signed)"
                        statusText.setTextColor(Color.parseColor("#C62828"))
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

    /**
     * The demo plays the Developer Portal: non-blank text typed in the boxes
     * rides on the session as `appearance`, the way an app's saved instruction
     * and privacy notice arrive from a real backend. Blank boxes leave the
     * backend's own text alone. The session's other bytes stay exactly as
     * issued: the merged `appearance` is appended as the last member, which the
     * SDK reads over an earlier one.
     */
    private fun withSavedText(session: String, instruction: String, notice: String): String {
        if (instruction.isEmpty() && notice.isEmpty()) return session
        val trimmed = session.trimEnd()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return session
        val appearance = runCatching { org.json.JSONObject(trimmed).optJSONObject("appearance") }.getOrNull()
            ?: org.json.JSONObject()
        if (instruction.isNotEmpty()) appearance.put("instruction", instruction)
        if (notice.isNotEmpty()) appearance.put("privacyMessage", notice)
        return trimmed.dropLast(1) + ",\"appearance\":" + appearance + "}"
    }

    private companion object {
        /** The last broker URL typed, so a phone pass needs it typed only once. */
        const val BROKER_URL_KEY = "brokerUrl"
        /** Used while the field is empty: the hosted broker on the stable demo domain (Very production sessions).
         *  The local one is `http://127.0.0.1:8799/sessions` through `adb reverse tcp:8799 tcp:8799`. */
        const val DEFAULT_BROKER_URL = "https://very-liveness-web.vercel.app/native-api/sessions"

        // Demonstrates the supported inline HTML: <b> for bold, <font color> for
        // colour, and <a href> for a tappable link.
        const val DEFAULT_PRIVACY_MESSAGE =
            "<b>The app will briefly capture your hand motion solely to confirm you're a real " +
            "person and not a bot.</b><br><br>Your hand motion is " +
            "<font color=\"#68C906\">never recorded as a video</font> and " +
            "it never leaves your device. It is not used for any other purpose other than " +
            "anti-bot verification and will not be sold, leased, or traded. " +
            "<a href=\"https://very.org/legal/privacy\">Learn more</a>"
    }
}
