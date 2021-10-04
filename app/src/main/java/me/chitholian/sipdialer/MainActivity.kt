package me.chitholian.sipdialer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import me.chitholian.sipdialer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), MainActivityActions {
    private lateinit var binding: ActivityMainBinding
    private lateinit var app: TheApp
    private var inputMode = Constants.INPUT_MODE_PHONE_NUMBER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        app = application as TheApp
        binding.actions = this
        binding.mode = inputMode

        app.state.reg.observe(this) { state ->
            binding.regState = state
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                Constants.REQ_RECORD_AUDIO
            )
        } else {
            app.startSipUa()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.REQ_RECORD_AUDIO -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    app.startSipUa()
                } else {
                    AlertDialog.Builder(this)
                        .setMessage("Audio record permission is required")
                        .setPositiveButton("Try Again") { d, _ ->
                            d.dismiss()
                            requestPermissions(
                                arrayOf(Manifest.permission.RECORD_AUDIO),
                                Constants.REQ_RECORD_AUDIO
                            )
                        }.setNegativeButton("Cancel") { d, _ ->
                            d.dismiss()
                        }.create().show()
                }
            }
        }
    }

    override fun actFor(act: Int) {
        when (act) {
            MainActivityActions.ACTION_OPEN_PREF -> startActivity(
                Intent(
                    this,
                    PreferencesActivity::class.java
                )
            )
            MainActivityActions.ACTION_OPEN_CONT -> startActivity(
                Intent(
                    Intent.ACTION_PICK,
                    ContactsContract.Contacts.CONTENT_URI
                )
            )
            MainActivityActions.ACTION_REG_RETRY -> app.createAccountIfPossible(true)
            MainActivityActions.ACTION_TOGGLE_MODE -> {
                inputMode =
                    if (inputMode == Constants.INPUT_MODE_PHONE_NUMBER) Constants.INPUT_MODE_SIP_ADDRESS
                    else Constants.INPUT_MODE_PHONE_NUMBER
                binding.mode = inputMode
            }
            MainActivityActions.ACTION_DIAL_NOW -> {
                val text = binding.inputDialStr.text?.trim() ?: ""
                if (text.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Please Input Number or Uri to Dial",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                app.initCall(text.toString())
                startActivity(Intent(this, CallActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.inputDialStr.requestFocus()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }
}

interface MainActivityActions {
    companion object {
        const val ACTION_OPEN_PREF = 1
        const val ACTION_OPEN_CONT = 2
        const val ACTION_REG_RETRY = 3
        const val ACTION_TOGGLE_MODE = 4
        const val ACTION_DIAL_NOW = 5
    }

    fun actFor(act: Int)
}
