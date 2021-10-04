package me.chitholian.sipdialer

import org.pjsip.pjsua2.Call
import org.pjsip.pjsua2.OnCallMediaStateParam
import org.pjsip.pjsua2.OnCallStateParam
import org.pjsip.pjsua2.pjmedia_type.PJMEDIA_TYPE_AUDIO
import org.pjsip.pjsua2.pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED
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
                call?.destroy()
                call = null
            }
            else -> {
                callState = CallState.STATE_ONGOING
            }
        }
        app.state.call.postValue(app.state.call.value?.apply {
            state = callState
            current = call
        })
        super.onCallState(prm)
    }

    override fun onCallMediaState(prm: OnCallMediaStateParam?) {
        for ((i, media) in info.media.withIndex()) {
            if (media.type == PJMEDIA_TYPE_AUDIO && media.status == PJSUA_CALL_MEDIA_ACTIVE || media.status == PJSUA_CALL_MEDIA_REMOTE_HOLD) {
                app.sipUa.startAudio(getAudioMedia(i))
            }
        }
        super.onCallMediaState(prm)
    }

    fun destroy() {
        delete()
    }
}
