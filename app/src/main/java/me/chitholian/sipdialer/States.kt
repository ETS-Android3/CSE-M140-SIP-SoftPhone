package me.chitholian.sipdialer

import androidx.lifecycle.MutableLiveData


class AppState {
    val reg: MutableLiveData<RegState> = MutableLiveData(RegState())
    val call: MutableLiveData<CallState> = MutableLiveData(CallState())
}

data class RegState(var registered: Boolean = false) {
    var reason: String? = null
    var pending = false
}

data class CallState(var state: Int = STATE_IDLE) {
    companion object {
        const val STATE_IDLE = 0
        const val STATE_DIALING = 1
        const val STATE_RINGING = 2
        const val STATE_INCOMING = 3
        const val STATE_ONGOING = 4
        const val STATE_PROCESSING = 5
    }

    var current: TheCall? = null
}
