package com.example.voiceassistant.ui.main

import android.app.Service
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.*
import android.os.Bundle
import android.os.IBinder
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.example.voiceassistant.Main2Activity
import com.example.voiceassistant.R
import com.example.voiceassistant.c2cService
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.frag1_fragment.*
import kotlinx.android.synthetic.main.frag1_fragment.view.*
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.io.*
import java.net.InetAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*


class Frag1 : Fragment() {

    companion object {
        fun newInstance() = Frag1()
    }

    private lateinit var viewModel: Frag1ViewModel

    // 1. declare a global webSocket //
    private var webSocket: WebSocket? = null

    // 1.2 declare a tcp socket and address //
    private var socket: Socket? = null
    private var databaseAddress: InetAddress? = null
    private var tcpBufferWrite: BufferedWriter? = null
    private var tcpBufferRead: BufferedReader? = null

    // 2. declare parameters for AudioRecord //
    private val AudioSource = MediaRecorder.AudioSource.MIC//Student source
    private val SampleRate = 16000//sampling rate
    private val Channel = AudioFormat.CHANNEL_IN_MONO//Mono channel
    private val EncodingType = AudioFormat.ENCODING_PCM_16BIT//data format
    private var bufferSizeInByte: Int = 0//Minimum recording buffer
    private var audioRecorder: AudioRecord? = null//Recording object
    private var isRecord = false
    private var recordingThread: Thread? = null

    // 2.2 declare parameters for AudioTrack //
    private val PlaySource = AudioManager.STREAM_MUSIC
    private var bufferSizeOutByte: Int = 0//Minimum playing buffer
    private var audioTracker: AudioTrack? = null

    // 3. others. Declare a button's flag to see if it's been pressed or not
    var connectionButtonFlag: Boolean = false
    var fileName: String = ""

    // 4. Patient's information
    class Patient {
        var chart_no: String = ""
        var name: String = ""
        var bp_systolic: Int = 0
        var bp_diastolic: Int = 0
        var temperature: Int = 0
        var pulse: Int = 0
        var respire: Int = 0
        var date: String = ""
    }

    // 5. Nurse's information
    var name: String = ""
    var ID: String = ""

    // 6. some variables related to FireBase
    //var database = FirebaseDatabase.getInstance()
    //var myRef = database.getReference()
    private lateinit var database: DatabaseReference

    // 7. c2c Service
    private var myC2cService: c2cService? = null
    private lateinit var serviceIntent:Intent


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        fileName = "${activity?.externalCacheDir?.absolutePath}/speechTextRecord"
        val view = inflater.inflate(R.layout.frag1_fragment, container, false)
        val button = view.button

        button.setOnClickListener { pressButton() }

        val textView = view.textView
        textView.movementMethod = ScrollingMovementMethod.getInstance()

        // Firebase callback function
        database = Firebase.database.reference.child("example")
        database.child("nurseLoginRecord").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val value = dataSnapshot.children
                value.forEach { Log.i("value:", it.key.toString() + it.value.toString()) }

                //Log.i("123", value.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
            }
        })

        // start c2c service
        serviceIntent = Intent(this.activity, c2cService::class.java)
        serviceIntent.putExtra("c2cAccount", "VOIP0001@jalabell.iptnet.net")
        serviceIntent.putExtra("c2cPassword", "voip1234")
        this.activity?.startService(serviceIntent)
        this.activity?.bindService(serviceIntent, mC2cServiceConnection, Service.BIND_AUTO_CREATE)

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(Frag1ViewModel::class.java)
        // TODO: Use the ViewModel
    }

    override fun onDestroy() {
        super.onDestroy()
        //this.activity?.stopService(serviceIntent)
    }

    var mC2cServiceConnection: ServiceConnection = object : ServiceConnection {
        // 成功與 Service 建立連線
        override fun onServiceConnected(name: ComponentName?, service: IBinder) {
            myC2cService = (service as c2cService.MyBinder).service
            Log.d(TAG, "MainActivity onServiceConnected")
        }

        // 與 Service 建立連線失敗
        override fun onServiceDisconnected(name: ComponentName?) {
            myC2cService = null
            Log.d(TAG, "MainActivity onServiceFailed")
        }
    }

    // show text on screen
    fun output(text: String) {
        activity?.runOnUiThread(Runnable {
            if(textView.text == "") {
                textView.text = text
            }
            else {
                var newString = ""
                val arrayofString = textView.text.split("\n")
                if(arrayofString.size > 7) {
                    val subArrayofString = arrayofString.subList(1, arrayofString.size)
                    newString = subArrayofString.joinToString("\n")

                }
                else {
                    newString = textView.text.toString()
                }
                textView.text = "${newString} \n $text"
            }
        })
    }

    fun sameCharNum(string1:String, string2:String): Int {
        var len1 = string1.length
        var len2 = string2.length
        var num = 0
        len1 = if(len1<len2) len1 else len2

        for(i in 0 until len1) {
            if(string1[i] == string2[i]) {
                num++
            }
        }
        return num
    }

    // =================== 解析語音文字，做對應的工作 (begin)=================== //
    var isSentenceOver:Boolean = false
    var wholeSpeech = ""
    fun parseCommandText(text: String) {
        if("登錄" in text) {
            output("登錄")
            if(name != "" && name != "Name" && ID != "" && ID != "ID") {
                var isInRecord = false
                var ref = database.child("nurseLoginRecord").orderByKey().equalTo("ID"+ ID)
                ref.addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.value != null) {
                            isInRecord = true

                            val dateformat = "yyyy/MM/dd kk:mm"
                            val df = SimpleDateFormat(dateformat)
                            val mCal: Calendar = Calendar.getInstance()
                            val now: String = df.format(mCal.time)

                            database.child("nurseLoginRecord").child("ID"+ ID).child("loginTime").setValue(now)
                        }
                        else {
                            output("this ID does not exist")
                        }
                        Log.i("real value:", dataSnapshot.toString())
                    }
                    override fun onCancelled(error: DatabaseError) {
                        // Failed to read value
                    }
                })

                /*val subJSONObject = JSONObject()
                subJSONObject.put("Name", name)
                subJSONObject.put("NurseNo", ID.toInt())
                subJSONObject.put("Command", "Login")

                myJsonString = subJSONObject.toString()

                thread = Thread(Runnable { sendDataToTcpServer(myJsonString) })
                thread.start()*/
            }
            else {
                output("請輸入 名字 和 ID ")
            }
        }
        else if("登出" in text) {
            var isInRecord = false
            var ref = database.child("nurseLoginRecord").orderByKey().equalTo("ID"+ ID)
            ref.addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.value != null && dataSnapshot.child("ID"+ ID).child("loginTime").value != null) {
                        isInRecord = true

                        val dateformat = "yyyy/MM/dd kk:mm"
                        val df = SimpleDateFormat(dateformat)
                        val mCal: Calendar = Calendar.getInstance()
                        val now: String = df.format(mCal.time)

                        database.child("nurseLoginRecord").child("ID"+ ID).child("logoutTime").setValue(now)
                    }
                    else {
                        output("this ID does not exist, or you haven't logged in yet.")
                    }
                    Log.i("real value:", dataSnapshot.toString())
                }
                override fun onCancelled(error: DatabaseError) {
                    // Failed to read value
                }
            })

            /*val subJSONObject = JSONObject()
            subJSONObject.put("Name", name)
            subJSONObject.put("NurseNo", ID.toInt())
            subJSONObject.put("Command", "Login")

            myJsonString = subJSONObject.toString()

            thread = Thread(Runnable { sendDataToTcpServer(myJsonString) })
            thread.start()*/
        }
        else if("打電話" in text) {
            output("打電話...")
            val intent = Intent(activity, Main2Activity::class.java).apply {
                putExtra("callerOrcallee", true)
                putExtra("callee", "VOIP0002@jalabell.iptnet.net")
            }
            startActivity(intent)

        }
        else if("記錄" in text) {

        }
        else if("修正" in text) {

        }
        else {
            /*if("患者編號" in text) {
                isSentenceOver = false
                database.child("command").setValue(text)
            }*/
            if(text.takeLast(2) == "結束") {
                isSentenceOver = true
                database.child("command").setValue(wholeSpeech + text)
                wholeSpeech = ""
            }
            else {
                wholeSpeech += text
                isSentenceOver = false
            }

        }

    }

    private val chineseNum = mutableMapOf<String,Int>("零" to 0, "一" to 1, "二" to 2, "兩" to 2, "三" to 3, "四" to 4, "五" to 5, "六" to 6, "七" to 7, "八" to 8, "九" to 9, "十" to 10, "百" to 100, "千" to 1000)

    private fun getTenDigit(num: Double) : Int{
        return num.toInt() % 10
    }

    private fun isNormalNumber(str: String) : Boolean {
        if("十" in str) {
            return true
        }
        if("百" in str) {
            return true
        }
        if("千" in str) {
            return true
        }
        return false
    }

    fun chineseToNum(str: String) : String {
        var newString: String = str
        var total = 0.0
        var numberBeginIndex = -1
        var flag = false
        var isRealNumber = false
        var isDecimal = false
        var decimalOrder = 0.1
        var numberEndIndex = str.length

        for(i in 0..(str.length-1)) {
            if(chineseNum.keys.contains(str[i].toString())) {
                if(isDecimal) {
                    total += chineseNum.getValue(str[i].toString()).toDouble()*decimalOrder
                    decimalOrder *= 0.1
                }
                else if(!flag) {
                    numberBeginIndex = i
                    flag = true

                    if(str[i] == '百') {
                        total += 100
                        isRealNumber = true
                    }
                    else if(str[i] == '千') {
                        total += 1000
                        isRealNumber = true
                    }
                    else {
                        if(isRealNumber) {
                            total += chineseNum.getValue(str[i].toString())
                        }
                        else {
                            total *= 10
                            total += chineseNum.getValue(str[i].toString())
                        }

                    }
                }
                else {
                    if(str[i] == '十') {
                        var tmp:Int = getTenDigit(total)
                        total -= tmp
                        total += (tmp*10)
                        isRealNumber = true
                    }
                    else if(str[i] == '百') {
                        var tmp:Int = getTenDigit(total)
                        total -= tmp
                        total += (tmp*100)
                        isRealNumber = true
                    }
                    else if(str[i] == '千') {
                        var tmp:Int = getTenDigit(total)
                        total -= tmp
                        total += (tmp*1000)
                        isRealNumber = true
                    }
                    else {
                        if(isRealNumber) {
                            total += chineseNum.getValue(str[i].toString())
                        }
                        else {
                            total *= 10
                            total += chineseNum.getValue(str[i].toString())
                        }
                    }
                }
            }
            else {
                if(str[i] == '點') {
                    isDecimal = true
                    decimalOrder = 0.1
                }
                else if(flag) {
                    if(isDecimal) {
                        newString = str.replace(str.substring(numberBeginIndex, i), total.toString())
                    }
                    else {
                        newString = str.replace(str.substring(numberBeginIndex, i), total.toInt().toString())
                    }
                    numberBeginIndex = -1
                    flag = false
                    isRealNumber = false
                    isDecimal = false
                    total = 0.0
                }
            }
        }

        if(numberBeginIndex != -1) {
            if(isDecimal) {
                newString = str.replace(str.substring(numberBeginIndex, str.length), total.toString())
            }
            else {
                newString = str.replace(str.substring(numberBeginIndex, str.length), total.toInt().toString())
            }
        }

        return newString
    }

    // =================== 解析語音文字，做對應的工作 (end)=================== //


    // ===================== WebSocket Section (begin) ===================== //
    // 1. create a websocket class 繼承 WebSocketListener
    inner class EchoWebSocketListener: WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            output("連線成功")

            // 開始錄音
            bufferSizeInByte = AudioRecord.getMinBufferSize(SampleRate, Channel, EncodingType)
            audioRecorder = AudioRecord(
                AudioSource, SampleRate, Channel,
                EncodingType, bufferSizeInByte
            )
            isRecord = true
            startRecording()

            output("開始錄音")
            activity?.runOnUiThread { button.isEnabled = true }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            super.onMessage(webSocket, bytes)
            output("onMessage byteString: $bytes")
        }

        var line = ""
        var recvTextData = ""
        var isAppendedFlag: Boolean = false   // check if the recvText is appended to wholeSpeech
        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            isAppendedFlag = false

            // receive the retured text from webserver
            val recvJSON = JSONObject(text)
            val jsonObjOfHypotheses = recvJSON.getJSONObject("result").getJSONArray("hypotheses").get(0) as JSONObject
            recvTextData = jsonObjOfHypotheses.getString("transcript")

            recvTextData = recvTextData.replace("\\s".toRegex(), "").replace(".", "")

            if(line!="") {
                if(sameCharNum(recvTextData, line) == 0 || sameCharNum(recvTextData,line) == recvTextData.length) {
                    //wholeSpeech = wholeSpeech + line + "\t"
                    isAppendedFlag = true
                    output("parse:$line")
                    parseCommandText(line)
                }
            }
            line = recvTextData

            output("onMessage: " + chineseToNum(line))
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)

            // 0. stop recording
            isRecord = false
            stopRecording()

            if(!isAppendedFlag) {
                //wholeSpeech = wholeSpeech + line + "\t"
            }

            /*// 1. create a json format which contains speech and patients' info
            val jsonObject = JSONObject()
            jsonObject.put("date", date)
            jsonObject.put("name", name)
            jsonObject.put("ID", ID)
            jsonObject.put("speech", wholeSpeech)

            // 2. convert json to String, and write it to file
            val jsonObjectString = jsonObject.toString() + "\n"
            var outputFile:FileOutputStream? = null

            try {
                outputFile = FileOutputStream(fileName, true)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }

            try {
                outputFile!!.write(jsonObjectString.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }

            outputFile!!.close()*/

            // 3. close tcp socket
            socket?.close()

            output("onClosed: $code/$reason")
            output("結束錄音")
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosing(webSocket, code, reason)
            output("onClosing: $code/$reason")
            output("結束錄音")
            if(isRecord) {
                isRecord = false
                stopRecording()

                socket?.close()
            }

            activity?.runOnUiThread { button.isEnabled = true }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            output("onFailure: " + t.localizedMessage)

            disconnect()
            isRecord = false
            stopRecording()
            socket?.close()

            connectionButtonFlag = false

            activity?.runOnUiThread { button.text = "連線"
                button.isEnabled = true }
        }
    }

    // 2. Use websocket to connect to the speech_to_text server.
    private fun connect() {
        // get patient's info and the current time
        name = editText.text.toString()
        ID = editText2.text.toString()

        // websocket initialization
        val webURL = "ws://asr.iptnet.net:8080/client/ws/speech?content-type=audio/x-raw,+layout=interleaved,+rate=16000,+format=S16LE,+channels=1"
        //val webURL = "ws://echo.websocket.org"
        val listener = EchoWebSocketListener()
        val request = Request.Builder().url(webURL).build()
        //val client = OkHttpClient()
        //val client = OkHttpClient.Builder().readTimeout(3, TimeUnit.SECONDS).build()
        val client = OkHttpClient.Builder().build()
        webSocket = client.newWebSocket(request, listener)

        // tcp socket initialization
        //val thread = Thread(Runnable { setTcpSocket() })
        //thread.start()

        textView.text = "連線..."
        //client.dispatcher().executorService().shutdown()
    }

    // 3. press '斷線0 button will invoke this disconnect function'
    private fun disconnect() {
        webSocket?.close(1000, "Bye")
    }

    private fun sendAudioDataToWebsocket() {
        val sData = ByteArray(bufferSizeInByte)
        while (isRecord) {
            audioRecorder?.read(sData, 0, bufferSizeInByte)
            webSocket?.send(ByteString.of(sData, 0, bufferSizeInByte))

        }
    }
    // ===================== WebSocket Section (end) ===================== //


    // ===================== Tcp Connection Section (begin) ===================== //
    private fun setTcpSocket() {
        // tcp socket initialization
        databaseAddress = InetAddress.getByName("192.168.0.105")
        try {
            socket = Socket(databaseAddress, 11000)
        } catch (e: IOException) {
            output("socket error:" + e.localizedMessage)
        }

        tcpBufferWrite = BufferedWriter(OutputStreamWriter(socket?.getOutputStream()!!))
        tcpBufferRead =  BufferedReader(InputStreamReader(socket?.getInputStream()!!))
    }

    fun sendDataToTcpServer(text: String) {
        if(socket?.isConnected!!) {
            try {
                tcpBufferWrite?.write(text)
                tcpBufferWrite?.flush()
            } catch (e: IOException) {
                output(e.localizedMessage)
            }
        }
    }

    fun receiveDataFromTcpServer() {
        var text: String = ""
        text = tcpBufferRead?.readLine()!!

    }

    private fun disconnectTcpSocket() {
        tcpBufferWrite?.close()
        tcpBufferRead?.close()
        socket?.close()
    }

    // ===================== Tcp Connection Section (end) ===================== //


    private fun startRecording() {
        audioRecorder?.startRecording()

        recordingThread = Thread(Runnable { sendAudioDataToWebsocket() }, "AudioRecorder Thread")
        recordingThread?.start()
    }

    private fun stopRecording() {
        audioRecorder?.stop()
        audioRecorder?.release()
        audioRecorder = null
    }

    // press '連線' button to invoke this function
    private fun pressButton() {
        if(!connectionButtonFlag) {
            button.isEnabled = false
            connectionButtonFlag = true
            button.text = "斷線"
            connect()

        }
        else {
            button.isEnabled = false
            connectionButtonFlag = false
            button.text = "連線"
            disconnect()


            // 3. close tcp socket
            //val thread = Thread(Runnable { disconnectTcpSocket() })
            //thread.start()
        }
    }

}
