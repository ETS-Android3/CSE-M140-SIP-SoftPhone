package me.chitholian.sipdialer

import android.net.Uri
import org.pjsip.pjsua2.Account
import org.pjsip.pjsua2.AccountConfig
import org.pjsip.pjsua2.AuthCredInfo
import org.pjsip.pjsua2.OnRegStateParam

class TheAccount(val app: TheApp, var username: String, var password: String) : Account() {
    private var initialized = false

    fun initialize(server: String) {
        if (initialized) destroy()
        val config = AccountConfig()
        config.idUri = Uri.parse("sip:$username@$server").toString()
        config.regConfig.registrarUri = Uri.parse("sip:$server").toString()
        config.regConfig.registerOnAdd = true
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

    override fun onRegState(prm: OnRegStateParam?) {
        super.onRegState(prm)
        println("On Reg Result: $prm, ${prm?.code}, ${prm?.status}, ${prm?.reason}")
    }
}
