package me.chitholian.sipdialer

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.pjsip.pjsua2.CallOpParam

class TheApp : Application(), SharedPreferences.OnSharedPreferenceChangeListener {
    lateinit var sipUa: SipUserAgent
    lateinit var prefs: SharedPreferences
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
            state.call.postValue(state.call.value?.apply {
                state = CallState.STATE_DIALING
                current = call
            })
            call.makeCall(destination, prm)
        }
    }
}
