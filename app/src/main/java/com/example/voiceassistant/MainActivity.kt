package com.example.voiceassistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.voiceassistant.ui.main.SectionsPagerAdapter
import java.io.FileInputStream
import java.io.FileNotFoundException

//private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
private const val REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 200

class MainActivity : AppCompatActivity() {

    //  1. Requesting permission to RECORD_AUDIO  //
    private var permissionToRecordAccepted = false
    private var permissionsForAudioRecord: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    /*override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            REQUEST_RECORD_AUDIO_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    permissionToRecordAccepted = true
                } else {
                    finish()
                }
                return
            }
            /*REQUEST_INTERNET_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    permissionToInternet = true
                } else {
                    finish()
                }
                return
            }*/
            else -> {
                // Ignore all other requests.
            }
        }
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)
        val fab: FloatingActionButton = findViewById(R.id.fab)


        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        InvokePermission()
        //ActivityCompat.requestPermissions(this, permissionsForAudioRecord, REQUEST_RECORD_AUDIO_PERMISSION)
    }

    private fun InvokePermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            val permissionsNeeded: MutableList<String> =
                mutableListOf<String>()
            val permissionsList: MutableList<String> = mutableListOf<String>()
            if (!addPermission(
                    permissionsList,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) permissionsNeeded.add("Read External Storage")
            if (!addPermission(
                    permissionsList,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) permissionsNeeded.add("Write External Storage")
            if (!addPermission(
                    permissionsList,
                    Manifest.permission.INTERNET
                )
            ) permissionsNeeded.add("INTERNET")
            if (!addPermission(
                    permissionsList,
                    Manifest.permission.RECORD_AUDIO
                )
            ) permissionsNeeded.add("RECORD AUDIO")
            if (!addPermission(
                    permissionsList,
                    Manifest.permission.MODIFY_AUDIO_SETTINGS
                )
            ) permissionsNeeded.add("MODIFY RECORD AUDIO")
            if (!addPermission(
                    permissionsList,
                    Manifest.permission.CAMERA
                )
            ) permissionsNeeded.add("CAMERA")
            if (permissionsList.size > 0) {
                requestPermissions(
                    permissionsList.toTypedArray(),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS
                )
                return
            }
            //		insertDummyContact();
        }
    }

    private fun addPermission(
        permissionsList: MutableList<String>,
        permission: String
    ): Boolean {
        if (this.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission)
            if (!shouldShowRequestPermissionRationale(permission)) return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS -> {
                val perms: MutableMap<String, Int> =
                    HashMap()
                perms[Manifest.permission.READ_EXTERNAL_STORAGE] = PackageManager.PERMISSION_GRANTED
                perms[Manifest.permission.WRITE_EXTERNAL_STORAGE] =
                    PackageManager.PERMISSION_GRANTED
                perms[Manifest.permission.INTERNET] = PackageManager.PERMISSION_GRANTED
                perms[Manifest.permission.RECORD_AUDIO] = PackageManager.PERMISSION_GRANTED
                perms[Manifest.permission.MODIFY_AUDIO_SETTINGS] = PackageManager.PERMISSION_GRANTED
                perms[Manifest.permission.CAMERA] = PackageManager.PERMISSION_GRANTED
                var i = 0
                while (i < permissions.size) {
                    perms[permissions[i]] = grantResults[i]
                    i++
                }
                if (perms[Manifest.permission.READ_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED && perms[Manifest.permission.WRITE_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED && perms[Manifest.permission.INTERNET] == PackageManager.PERMISSION_GRANTED && perms[Manifest.permission.RECORD_AUDIO] == PackageManager.PERMISSION_GRANTED && perms[Manifest.permission.MODIFY_AUDIO_SETTINGS] == PackageManager.PERMISSION_GRANTED && perms[Manifest.permission.CAMERA] == PackageManager.PERMISSION_GRANTED
                ) {
                    // All Permissions Granted
//					insertDummyContact();
                } else {
                    // Permission Denied
                    Toast.makeText(
                        this@MainActivity,
                        "Some Permission is Denied",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

}