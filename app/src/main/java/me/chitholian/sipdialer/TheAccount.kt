package me.chitholian.sipdialer

import android.net.Uri
import org.pjsip.pjsua2.*
import org.pjsip.pjsua2.pjsip_status_code.PJSIP_SC_OK

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
        app.state.reg.postValue(
            app.state.reg.value?.apply {
                registered = prm?.code == 200 && prm.expiration > 0
                reason = prm?.reason
                pending = false
            }
        )
    }

    override fun onIncomingCall(prm: OnIncomingCallParam?) {
        super.onIncomingCall(prm)
        val call = TheCall(this.app, this, prm?.callId ?: -1)
        val callPrm = CallOpParam(true)

        // Check if a call is active, then reply busy.
        if (app.state.call.value?.state != CallState.STATE_IDLE) {
            callPrm.statusCode = 486
            call.answer(callPrm)
            call.destroy()
        }
        callPrm.statusCode = PJSIP_SC_OK
        call.answer(callPrm)
        app.state.call.postValue(app.state.call.value?.apply {
            state = CallState.STATE_INCOMING
            current?.destroy()
            current = call
        })
    }
}
