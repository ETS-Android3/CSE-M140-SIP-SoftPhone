package me.chitholian.sipdialer

import android.app.KeyguardManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import me.chitholian.sipdialer.databinding.ActivityCallBinding
import org.pjsip.pjsua2.CallOpParam
import org.pjsip.pjsua2.pjsua_call_flag
import java.util.*
import java.util.concurrent.TimeUnit

class CallActivity : AppCompatActivity(), CallScreenFunctions {
    private lateinit var binding: ActivityCallBinding
    private lateinit var app: TheApp
    private var clockTimer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as TheApp
        binding = DataBindingUtil.setContentView(this, R.layout.activity_call)

        app.state.call.observe(this) { c ->
            if (c.state == CallState.STATE_IDLE) {
                Handler(mainLooper).postDelayed({
                    finish()
                }, Constants.CALL_END_DELAY)
            }
            binding.callState = c
        }
        binding.func = this
        // Turn screen ON
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
            setShowWhenLocked(true)
            val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        }
    }

    override fun getStatusText(state: CallState): String {
        return when (state.state) {
            CallState.STATE_IDLE -> {
                if (state.lastStatusCode == 200) "Call Ended" else "${state.lastStatusCode} ${state.lastStatusText}"
            }
            CallState.STATE_INCOMING -> "Incoming Call"
            CallState.STATE_DIALING -> "Dialing"
            CallState.STATE_RINGING -> "Ringing"
            CallState.STATE_PROCESSING -> "Processing"
            CallState.STATE_ONGOING -> "Call Active"
            else -> "Call Status Unknown"
        }
    }

    override fun getCallDuration(state: CallState): String {
        val millis = System.currentTimeMillis() - state.timeStart
        return String.format(
            "%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(millis),
            TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(
                TimeUnit.MILLISECONDS.toHours(millis)
            ),
            TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(
                TimeUnit.MILLISECONDS.toMinutes(millis)
            )
        )
    }

    override fun answerCall() {
        app.stopRingTone()
        val prm = CallOpParam(true)
        prm.statusCode = 200
        app.state.call.value?.current?.answer(prm)
    }

    override fun toggleSpeakerMode() {
        var speakerMode = false
        app.state.call.value?.let {
            speakerMode = !it.isSpeakerMode
        }
        app.setSpeakerMode(speakerMode)
        app.state.call.postValue(app.state.call.value?.apply {
            isSpeakerMode = speakerMode
        })
    }

    override fun toggleMute() {
        var muted = false
        app.state.call.value?.let {
            muted = !it.isMuted
        }
        app.setMicMode(muted)
        app.state.call.postValue(app.state.call.value?.apply {
            isMuted = muted
        })
    }

    override fun toggleHold() {
        var held = false
        val prm = CallOpParam(true)
        app.state.call.value?.let {
            if (it.isOnHold) {
                val setting = prm.opt
                setting.audioCount = 1
                setting.videoCount = 0
                setting.flag = pjsua_call_flag.PJSUA_CALL_UNHOLD.toLong()
                it.current?.reinvite(prm)
                held = false
            } else {
                it.current?.setHold(prm)
                held = true
            }
        }
        app.state.call.postValue(app.state.call.value?.apply {
            isOnHold = held
        })
    }

    override fun hangupCall() {
        app.state.call.value?.let {
            val prm = CallOpParam(true)
            if (it.state == CallState.STATE_INCOMING) {
                prm.statusCode = 486
                app.stopRingTone()
            }
            it.current?.hangup(prm)
        }
    }

    override fun onStart() {
        super.onStart()
        clockTimer?.cancel()
        clockTimer = Timer()
        clockTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    binding.invalidateAll()
                }
            }
        }, 0, 1000)
    }

    override fun onStop() {
        clockTimer?.cancel()
        super.onStop()
    }
}

interface CallScreenFunctions {
    fun getStatusText(state: CallState): String
    fun getCallDuration(state: CallState): String
    fun answerCall()
    fun toggleSpeakerMode()
    fun toggleMute()
    fun toggleHold()
    fun hangupCall()
}
