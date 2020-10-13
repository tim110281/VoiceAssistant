package com.example.voiceassistant

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.iptnet.voipframework.*

class c2cService : Service() {

    var myBinder = MyBinder()

    // 綁定此 Service 的物件
    inner class MyBinder : Binder() {
        val service: c2cService
            get() = this@c2cService
    }

    // ======================= c2c (begin) ======================= //

    lateinit var voip: VOIP
    lateinit var callout_vhd: VOIPSessionInfo
    lateinit var callin_vhd: VOIPSessionInfo

    //private lateinit var calleeid:String

    var callOrAnswerFlag:Boolean = false    //true for caller, false for callee

    private val mOnLoginListener: VOIPCallbacks.OnLoginListener = object : VOIPCallbacks.OnLoginListener() {

        override fun onLogining(info: RegisterInfo) {
            Log.i("Msg:", "Logining")
        }
        override fun onLoginSuccess(info: RegisterInfo) {
            Log.i("Msg:", "LoginSuccess")
        }
        override fun onLoginFailure(info: RegisterInfo) {
            Log.i("Msg:", "LoginFailure " + info.errorMsg)
        }
        override fun onLogoutByServer(info: RegisterInfo) {}
        override fun onLogoutDone(info: RegisterInfo) {}
        override fun onLogoutFailed(info: RegisterInfo) {}
    }

    private val mOnVOIPSessionListener: VOIPCallbacks.OnVOIPSessionListener = object : VOIPCallbacks.OnVOIPSessionListener() {
        override fun onOutgoing(info: VOIPSessionInfo) {
            Log.i("Msg:", "Outgoing")
        }
        override fun onRinging(info: VOIPSessionInfo) {
            Log.i("Msg:", "Ringing")
        }
        override fun onIncoming(info: VOIPSessionInfo) {
            Log.i("Msg:", "Incoming")
            callin_vhd = info

            val intent = Intent(this@c2cService, Main2Activity::class.java)
            intent.putExtra("callerOrcallee", false)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        override fun onAnswer(info: VOIPSessionInfo) {
            Log.i("Msg:", "Answer")
            voip.startAudioPlayAndRec(this@c2cService)
            voip.startPlayVideo()
            voip.startRecordVideo()
        }
        override fun onReceiveAudio(info: VOIPSessionInfo, frame: BaseFrame) {
        }
        override fun onReceiveVideo(info: VOIPSessionInfo, frame: BaseFrame) {
        }
        override fun onHold(info: VOIPSessionInfo) {}
        override fun onTransfer(info: VOIPSessionInfo) {}
        override fun onReceiveAlert(info: VOIPSessionInfo) {}
        override fun onTerminated(info: VOIPSessionInfo) {
            when (info.message) {
                VOIP.VOIPMainEvents.VOIP_404_EVENT -> {
                }
                VOIP.VOIPMainEvents.VOIP_4XX_EVENT -> {
                }
                VOIP.VOIPMainEvents.VOIP_NOANSWER_EVENT -> {
                }
                VOIP.VOIPMainEvents.VOIP_BUSY_EVENT -> {
                }
                VOIP.VOIPMainEvents.VOIP_PACKET_LOSS_EVENT -> {
                }
                VOIP.VOIPMainEvents.VOIP_CANCELED_EVENT -> {
                }
                VOIP.VOIPMainEvents.VOIP_CALL_TERMINATED_EVENT -> {
                }
                VOIP.VOIPMainEvents.VOIP_OUTGOING_ERROR_EVENT -> {
                    when (info.errorNo) {
                        VOIP.VOIPSubEvents.VOIP_REMOTE_NO_RESP -> {
                        }
                        VOIP.VOIPSubEvents.VOIP_TIMEOUT -> {
                        }
                        VOIP.VOIPSubEvents.VOIP_SRV_DISCONNECT -> {
                        }
                        VOIP.VOIPSubEvents.VOIP_RELAY_NO_RESP -> {
                        }
                        VOIP.VOIPSubEvents.VOIP_RELAY_FAIL -> {
                        }
                        VOIP.VOIPSubEvents.VOIP_PENDING_TIMEOUT -> {
                        }
                        VOIP.VOIPSubEvents.VOIP_SRV_NO_RESP -> {
                        }
                        VOIP.VOIPSubEvents.VOIP_UNAUTHORIZED -> {
                        }
                        VOIP.VOIPSubEvents.VOIP_SOCKET_ERROR -> {
                        }
                        VOIP.VOIPSubEvents.VOIP_LOCAL_BUSY -> {
                        }
                        else -> {
                        }
                    }
                }
                else -> {
                }
            }
            voip.stopAudioPlayAndRec()
            voip.stopPlayVideo()
            voip.stopRecordVideo()
        }
    }

    fun call(calleeid:String) {
        callout_vhd = voip.createNewSession()
        val ret = voip.call(callout_vhd, calleeid)
        if ( ret < 0)
        {
            // show failed message if call failed.
            voip.getErrorMsg(ret)
            Log.i("call", "fail")
        }
        else
        {
            Log.i("call", "succeed")
        }
    }

    fun answer() {
        val ret = voip.answer(callin_vhd)
        if (ret < 0) {
            Log.i("answer", "fail")
        }
        else {
            Log.i("anwser", "succeed")
        }
    }

    // ======================= c2c (end) ======================= //

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val account = intent?.getStringExtra("c2cAccount")
        val password = intent?.getStringExtra("c2cPassword")

        voip = VOIP.sharedInstance()
        voip.initialize()

        //voip.setDebugOn(true)

        voip.addOnLoginListener(mOnLoginListener)

        if (account != null && password != null) {
            voip.login( "jalabell.iptnet.net", account, password)
        }

        voip.addOnVOIPSessionListener(mOnVOIPSessionListener)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        return myBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
