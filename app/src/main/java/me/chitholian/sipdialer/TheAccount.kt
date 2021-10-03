package me.chitholian.sipdialer

import android.net.Uri
import org.pjsip.pjsua2.*

class TheAccount(val app: TheApp, var username: String, var password: String) : Account() {
    private var initialized = false

    fun initialize(server: String) {
        if (initialized) destroy()
        val config = AccountConfig()
        config.idUri = Uri.parse("sip:$username@$server").toString()
        config.regConfig.registrarUri = Uri.parse("sip:$server").toString()
        config.regConfig.registerOnAdd = true
        config.regConfig.retryIntervalSec = 5
        val cred = AuthCredInfo("digest", "*", username, 0, password)
        config.sipConfig.authCreds.add(cred)
        create(config, true)
        initialized = true
    }

    fun destroy() {
        if (initialized) {
            delete()
            initialized = false
        }
    }

    override fun onRegStarted(prm: OnRegStartedParam?) {
        super.onRegStarted(prm)
        app.state.reg.postValue(
            app.state.reg.value?.apply {
                pending = true
                registered = false
                reason = "Pending"
            }
        )
    }

    override fun onRegState(prm: OnRegStateParam?) {
        super.onRegState(prm)
        println("On Reg Result: $prm, ${prm?.code}, ${prm?.status}, ${prm?.reason}")
        app.state.reg.postValue(
            app.state.reg.value?.apply {
                registered = prm?.code == 200
                reason = prm?.reason
                pending = false
            }
        )
    }
}
