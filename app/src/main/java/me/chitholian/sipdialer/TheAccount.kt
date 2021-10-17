package me.chitholian.sipdialer

import android.content.Intent
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

        // Check if a call is active, then reply busy.
        if (app.state.call.value?.state != CallState.STATE_IDLE) {
            val callPrm = CallOpParam(true)
            callPrm.statusCode = 486
            call.answer(callPrm)
            return
        }
        app.state.call.postValue(CallState(CallState.STATE_INCOMING).apply {
            current = call
            remoteContact = (call.info.remoteUri ?: "").trim('<', '>')
        })
        // Start Service
        app.startService(Intent(app, CallService::class.java))
        // Show CallScreen
        app.startActivity(Intent(app, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        // Play RING
        app.playRingTone()
    }
}
