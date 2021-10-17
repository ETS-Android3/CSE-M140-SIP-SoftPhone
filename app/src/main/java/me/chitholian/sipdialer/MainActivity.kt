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
import java.lang.Exception

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK) {
            data?.data?.apply {
                val proj = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
                contentResolver.query(this, proj, null, null, null).use { c ->
                    if (c?.moveToFirst() == true) {
                        val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        var number = c.getString(numIdx)
                        number = number.replace(" ", "")
                        number = number.replace("-", "")
                        dialNow(number)
                    }
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
            MainActivityActions.ACTION_OPEN_CONT -> {
                val intent = Intent(Intent.ACTION_PICK).apply {
                    type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                }
                /*if(intent.resolveActivity(packageManager) != null) {
                    startActivityForResult(intent, 101)
                }*/
                startActivityForResult(intent, 101)
            }
            MainActivityActions.ACTION_REG_RETRY -> app.createAccountIfPossible(true)
            MainActivityActions.ACTION_TOGGLE_MODE -> {
                inputMode =
                    if (inputMode == Constants.INPUT_MODE_PHONE_NUMBER) Constants.INPUT_MODE_SIP_ADDRESS
                    else Constants.INPUT_MODE_PHONE_NUMBER
                binding.mode = inputMode
            }
            MainActivityActions.ACTION_DIAL_NOW -> {
                val text = binding.inputDialStr.text?.trim() ?: ""
                dialNow(text.toString())
            }
        }
    }

    private fun dialNow(text: String) {
        var sipUri = ""
        if (text.isEmpty()) {
            Toast.makeText(
                this,
                "Please Input Number or Uri to Dial",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        // Check Input is Number or SIP URI
        if (text.matches(Regex("^[a-zA-Z0-9_+*#.,;-]+$"))) {
            // Only Number is Given. Add protocol and server address.
            val server = app.prefs.getString(Constants.KEY_SERVER, "")
            if (server?.isNotEmpty() != true) {
                AlertDialog.Builder(this)
                    .setMessage("No SIP server configured, please enter SIP Uri")
                    .setPositiveButton("OK") { d, _ ->
                        d.dismiss()
                    }.create().show()
                return
            }
            sipUri = "sip:$text@$server"
        } else if (text.matches(Regex("^[a-zA-Z0-9_+*#.,;-]+@[a-zA-Z0-9_+.-]+(:[0-9]{1,5})?$"))) {
            // Add protocol.
            sipUri = "sip:$text"
        } else if (!text.matches(Regex("^sips?:[a-zA-Z0-9_+*#.,;-]+@[a-zA-Z0-9_+.-]+(:[0-9]{1,5})?$"))) {
            // Invalid Input.
            Toast.makeText(
                this,
                "Please Input a Valid Number or Uri to Dial",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        try {
            app.initCall(sipUri)
        } catch (e: Exception) {
            AlertDialog.Builder(this)
                .setMessage("Could not create call, error: ${e.message}")
                .setPositiveButton("OK") { d, _ ->
                    d.dismiss()
                }.create().show()
            e.printStackTrace()
            return
        }
        startActivity(Intent(this, CallActivity::class.java))
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
