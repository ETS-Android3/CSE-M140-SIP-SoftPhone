package me.chitholian.sipdialer

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class TheApp : Application(), SharedPreferences.OnSharedPreferenceChangeListener {
    lateinit var sipUa: SipUserAgent
    lateinit var prefs: SharedPreferences
    private var account: TheAccount? = null

    override fun onCreate() {
        super.onCreate()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        sipUa = SipUserAgent(this)
    }

    override fun onTerminate() {
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

    private fun createAccountIfPossible(force: Boolean = false) {
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
}
