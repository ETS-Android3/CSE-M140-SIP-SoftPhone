package me.chitholian.sipdialer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import me.chitholian.sipdialer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), MainActivityActions {
    private lateinit var binding: ActivityMainBinding
    private lateinit var app: TheApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        app = application as TheApp
        binding.actions = this

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
        }
    }
}

interface MainActivityActions {
    companion object {
        const val ACTION_OPEN_PREF = 1
        const val ACTION_OPEN_CONT = 2
        const val ACTION_REG_RETRY = 3
    }

    fun actFor(act: Int)
}
