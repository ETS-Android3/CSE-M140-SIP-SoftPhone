package me.chitholian.sipdialer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import me.chitholian.sipdialer.databinding.ActivityCallBinding

class CallActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCallBinding
    private lateinit var app: TheApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as TheApp
        binding = DataBindingUtil.setContentView(this, R.layout.activity_call)

        app.state.call.observe(this) { c ->
            if (c.state == CallState.STATE_IDLE) {
                finish()
            }
        }
    }
}
