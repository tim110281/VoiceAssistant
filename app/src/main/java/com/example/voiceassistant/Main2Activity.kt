package com.example.voiceassistant

import android.Manifest
import android.app.Service
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.iptnet.voipframework.*
import kotlinx.android.synthetic.main.activity_main2.*
import kotlinx.android.synthetic.main.frag1_fragment.*

class Main2Activity : AppCompatActivity() {

    private var myC2cService: c2cService? = null
    private var callerOrcallee: Boolean = false // true for caller; false for callee

    /*private lateinit var callout_vhd:VOIPSessionInfo
    private lateinit var callin_vhd:VOIPSessionInfo*/

    fun output(text: String) {
        this.runOnUiThread(Runnable {
            if(textView2.text == "") {
                textView2.text = text
            }
            else {
                var newString = ""
                val arrayofString = textView2.text.split("\n")
                if(arrayofString.size > 7) {
                    val subArrayofString = arrayofString.subList(1, arrayofString.size)
                    newString = subArrayofString.joinToString("\n")

                }
                else {
                    newString = textView2.text.toString()
                }
                textView2.text = "${newString} \n $text"
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        callerOrcallee = intent.getBooleanExtra("callerOrcallee", false)

        // bind c2c service
        var serviceIntent = Intent(this, c2cService::class.java)

        this.bindService(serviceIntent, mC2cServiceConnection, Service.BIND_AUTO_CREATE)

        button2.isEnabled = false
        button3.isEnabled = false
        button2.setOnClickListener { hangup() }
        button3.setOnClickListener { answer() }

        val intentFilter = IntentFilter("voipMsg")
        registerReceiver(voipStateReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    var mC2cServiceConnection: ServiceConnection = object : ServiceConnection {
        // 成功與 Service 建立連線
        override fun onServiceConnected(name: ComponentName?, service: IBinder) {
            myC2cService = (service as c2cService.MyBinder).service

            this@Main2Activity.runOnUiThread {
                button2.isEnabled = true
                button3.isEnabled = true
            }

            myC2cService?.voip?.setRemoteView(surfaceView)
            myC2cService?.voip?.setLocalView(surfaceView2)


            val r = Runnable {
                if(callerOrcallee) {
                    val callee = intent.getStringExtra("callee")
                    myC2cService?.call(callee)
                    button3.isEnabled = false
                }
            }
            Handler().postDelayed(r,3000)

            if(callerOrcallee) {
                val callee = intent.getStringExtra("callee")
                myC2cService?.call(callee)
                button3.isEnabled = false
            }
            Log.i("bindService", "Main2Activity bind c2cService successfully.")
        }

        // 與 Service 建立連線失敗
        override fun onServiceDisconnected(name: ComponentName?) {
            myC2cService = null
            Log.i("bindService", "Main2Activity bind c2cService fail.")
        }
    }



    /*private fun hangup() {
        if(callerOrcallee) {
            myC2cService?.voip?.hangUp(myC2cService?.callout_vhd!!)
        }
        else {
            myC2cService?.voip?.hangUp(myC2cService?.callin_vhd!!)
        }
        Log.i("Msg:", "Main2Activity hangup")

        //this.onDestroy()
    }*/

    private fun hangup() {
        myC2cService?.hangup(callerOrcallee)
    }



    private fun answer() {
        button3.isEnabled = false
        myC2cService?.answer()
    }

    private val voipStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if ("voipMsg" == intent.action) {
                if (intent.getBooleanExtra("state", false)) {
                    this@Main2Activity.unbindService(mC2cServiceConnection)
                    this@Main2Activity.finish()
                }
                Log.i("TESTING:", "receive state")
            }
        }
    }
}
