package me.chitholian.sipdialer

import android.app.Application
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import androidx.preference.PreferenceManager
import org.pjsip.pjsua2.CallOpParam
import java.lang.Exception

class TheApp : Application(), SharedPreferences.OnSharedPreferenceChangeListener {
    lateinit var sipUa: SipUserAgent
    lateinit var prefs: SharedPreferences
    private var ringtonePlayer: MediaPlayer? = null
    private var account: TheAccount? = null
    val state = AppState()

    override fun onCreate() {
        super.onCreate()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(this)
        sipUa = SipUserAgent(this)
    }

    override fun onTerminate() {
        state.call.postValue(state.call.value?.apply {
            current?.destroy()
            current = null
            state = CallState.STATE_IDLE
        })
        account?.destroy()
        account = null
        sipUa.destroy()
        ringtonePlayer?.release()
        super.onTerminate()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            Constants.KEY_USERNAME, Constants.KEY_PASSWORD, Constants.KEY_SERVER -> {
                createAccountIfPossible(true)
            }
        }
    }

    fun createAccountIfPossible(force: Boolean = false) {
        if (account != null && !force) return
        val username = prefs.getString(Constants.KEY_USERNAME, "") ?: ""
        val server = prefs.getString(Constants.KEY_SERVER, "") ?: ""

        if (username.isNotEmpty() && server.isNotEmpty()) {
            val password = prefs.getString(Constants.KEY_PASSWORD, "") ?: ""
            account?.destroy()
            account = TheAccount(this, username, password)
            account?.initialize(server)
        } else {
            account?.destroy()
            account = null
        }
    }

    fun startSipUa() {
        sipUa.initialize()
        createAccountIfPossible()
    }

    fun initCall(destination: String) {
        if (account != null && state.call.value?.state == CallState.STATE_IDLE) {
            val call = TheCall(this, account!!, -1)
            val prm = CallOpParam(true)
            state.call.value = (CallState(CallState.STATE_DIALING).apply {
                current = call
            })
            call.makeCall(destination, prm)
        }
    }

    fun playRingTone() {
        ringtonePlayer?.stop()
        ringtonePlayer?.release()
        ringtonePlayer = MediaPlayer.create(
            this,
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
        )
        ringtonePlayer?.isLooping = true
        ringtonePlayer?.start()
    }

    fun stopRingTone() {
        ringtonePlayer?.stop()
        ringtonePlayer?.release()
        ringtonePlayer = null
    }

    fun setSpeakerMode(turnOn: Boolean) {
        try {
            sipUa.setSpeakerMode(turnOn)
        } catch (e: Exception) {
            val am = getSystemService(AUDIO_SERVICE) as AudioManager
            am.isSpeakerphoneOn = turnOn
        }
    }
}
