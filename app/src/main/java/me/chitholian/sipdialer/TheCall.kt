package me.chitholian.sipdialer

import android.content.Intent
import org.pjsip.pjsua2.Call
import org.pjsip.pjsua2.OnCallMediaStateParam
import org.pjsip.pjsua2.OnCallRxReinviteParam
import org.pjsip.pjsua2.OnCallStateParam
import org.pjsip.pjsua2.pjmedia_type.PJMEDIA_TYPE_AUDIO
import org.pjsip.pjsua2.pjsip_inv_state.*
import org.pjsip.pjsua2.pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE
import org.pjsip.pjsua2.pjsua_call_media_status.PJSUA_CALL_MEDIA_REMOTE_HOLD

class TheCall(val app: TheApp, val account: TheAccount, callId: Int) : Call(account, callId) {

    override fun onCallState(prm: OnCallStateParam?) {
        app.state.call.value?.let {
            if (it.current == null || it.current?.id != id) return
        }

        var call: TheCall? = app.state.call.value?.current
        var callState = CallState.STATE_IDLE

        when (info.state) {
            PJSIP_INV_STATE_DISCONNECTED -> {
                callState = CallState.STATE_IDLE
                call = null
                app.stopService(Intent(app, CallService::class.java))
                app.stopRingTone()
                app.setSpeakerMode(false)
            }
            PJSIP_INV_STATE_EARLY -> {
                callState = CallState.STATE_RINGING
            }
            PJSIP_INV_STATE_CONNECTING -> {
                callState = CallState.STATE_PROCESSING
            }
            PJSIP_INV_STATE_CONFIRMED -> {
                callState = CallState.STATE_ONGOING
            }
            PJSIP_INV_STATE_CALLING -> {
                callState = CallState.STATE_DIALING
                app.startService(Intent(app, CallService::class.java))
            }
        }
        app.state.call.postValue(app.state.call.value?.apply {
            state = callState
            current = call
            lastStatusCode = info.lastStatusCode
            lastStatusText = info.lastReason
            if (info.state == PJSIP_INV_STATE_CONFIRMED) {
                timeStart = System.currentTimeMillis()
                connected = true
            }
            remoteContact = info.remoteUri.trim('<', '>')
        })
        super.onCallState(prm)
    }

    override fun onCallMediaState(prm: OnCallMediaStateParam?) {
        var remoteHold = false
        for ((i, media) in info.media.withIndex()) {
            if (media.type == PJMEDIA_TYPE_AUDIO && media.status == PJSUA_CALL_MEDIA_ACTIVE || media.status == PJSUA_CALL_MEDIA_REMOTE_HOLD) {
                app.sipUa.startAudio(getAudioMedia(i))
            }
            remoteHold = remoteHold || media.status == PJSUA_CALL_MEDIA_REMOTE_HOLD
        }
        app.state.call.postValue(app.state.call.value?.apply {
            isRemoteHold = remoteHold
        })
        super.onCallMediaState(prm)
    }

    override fun onCallRxReinvite(prm: OnCallRxReinviteParam?) {
        super.onCallRxReinvite(prm)
    }

    fun destroy() {
        delete()
    }
}
