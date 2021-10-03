package me.chitholian.sipdialer

import androidx.lifecycle.MutableLiveData


class AppState {
    val reg: MutableLiveData<RegState> = MutableLiveData(RegState())
}

data class RegState(var registered: Boolean = false) {
    var reason: String? = null
    var pending = false
}
