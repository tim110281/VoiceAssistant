package com.example.voiceassistant.ui.main

import android.media.*
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.example.voiceassistant.R
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

    // 3. others. button's flag to see if it's been pressed or not
    var flag: Boolean = false
    var fileName: String = ""

    // 4. record the information of patients
    class Patient {
        var chart_no: String = ""
        var name: String = ""
        var bp_systolic: Int = 0
        var bp_diastolic: Int = 0
        var temperature: Int = 0
        var pulse: Int = 0
        var respire: Int = 0
    }

    var name: String = ""
    var ID: String = ""
    var date: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        fileName = "${activity?.externalCacheDir?.absolutePath}/speechTextRecord"
        val view = inflater.inflate(R.layout.frag1_fragment, container, false)
        val button = view.button

        button.setOnClickListener { pressButton() }

        val textView = view.textView
        textView.movementMethod = ScrollingMovementMethod.getInstance()

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(Frag1ViewModel::class.java)
        // TODO: Use the ViewModel
    }

    // show text on screen
    fun output(text: String) {
        activity?.runOnUiThread(Runnable {
            val newText = "${textView.text.toString()} \n $text"
            textView.text = newText
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
    fun parseCommandText(text: String) {
        lateinit var thread: Thread
        var myJsonString: String = ""

        if("登錄" in text) {
            if(name != "" && name != "Name" && ID != "" && ID != "ID") {
                val subJSONObject = JSONObject()
                subJSONObject.put("Name", name)
                subJSONObject.put("NurseNo", ID.toInt())
                subJSONObject.put("Command", "Login")

                myJsonString = subJSONObject.toString()

                thread = Thread(Runnable { sendDataToTcpServer(myJsonString) })
                thread.start()
            }
            else {
                output("請輸入 名字 和 ID ")
            }
        }
        else if("登出" in text) {
            val subJSONObject = JSONObject()
            subJSONObject.put("Name", name)
            subJSONObject.put("NurseNo", ID.toInt())
            subJSONObject.put("Command", "Login")

            myJsonString = subJSONObject.toString()

            thread = Thread(Runnable { sendDataToTcpServer(myJsonString) })
            thread.start()
        }
        else if("打電話" in text) {

        }
        else if("記錄" in text) {

        }
        else if("修正" in text) {

        }

    }

    val chineseNum:String = "零一二三四五六七八九十百千"
    fun chineseToNum(str: String) : String {
        var newString: String = ""
        var total:Int = 0
        var numberBeginIndex = -1
        var flag = false
        for(i in 0..(str.length-1)) {
            if(str[i] in chineseNum) {
                if(!flag) {
                    numberBeginIndex = i
                    flag = true

                    if(str[i] == '百') {
                        total += 100
                    }
                    else if(str[i] == '千') {
                        total += 1000
                    }
                    else {
                        total += chineseNum.indexOf(str[i],0)
                    }
                }
                else {
                    if(str[i] == '十') {
                        total *= 10
                    }
                    else if(str[i] == '百') {
                        total *= 100
                    }
                    else if(str[i] == '千') {
                        total *= 1000
                    }
                    else {
                        total += chineseNum.indexOf(str[i],0)
                    }
                }
            }
            else {
                if(flag) {
                    newString = str.replace(str.substring(numberBeginIndex, i-1), total.toString())
                    numberBeginIndex = -1
                    flag = false
                    total = 0
                }
            }
        }

        if(numberBeginIndex != -1) {
            output(total.toString())
            newString = str.replace(str.substring(numberBeginIndex, str.length), total.toString())

            output(newString)
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
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            super.onMessage(webSocket, bytes)
            output("onMessage byteString: $bytes")
        }

        var wholeSpeech = ""
        var line = ""
        var recvTextData = ""
        var flag: Boolean = false   // check if the recvText is appended to wholeSpeech
        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            flag = false

            // receive the retured text from webserver
            val recvJSON = JSONObject(text)
            val jsonObjOfHypotheses = recvJSON.getJSONObject("result").getJSONArray("hypotheses").get(0) as JSONObject
            recvTextData = jsonObjOfHypotheses.getString("transcript")

            recvTextData = recvTextData.replace("\\s".toRegex(), "").replace(".", "")

            if(line!="") {
                if(sameCharNum(recvTextData, line) == 0 || sameCharNum(recvTextData,line) == recvTextData.length) {
                    wholeSpeech = wholeSpeech + line + "\t"
                    flag = true
                    parseCommandText(line)
                }
            }
            line = recvTextData


            output("onMessage: $line")
            output("onMessage: " + chineseToNum(line))
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)

            // 0. stop recording
            output("onClosed: $code/$reason")
            output("結束錄音")
            isRecord = false
            stopRecording()

            if(!flag) {
                wholeSpeech = wholeSpeech + line + "\t"
            }

            // 1. create a json format which contains speech and patients' info
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

            outputFile!!.close()

            // 3. close tcp socket
            socket?.close()
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
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            output("onFailure: " + t.localizedMessage)

            disconnect()
            isRecord = false
            stopRecording()
            socket?.close()

        }
    }

    // 2. pressing '連線' button will invoke this connect function
    private fun connect() {
        // get patient's info and the current time
        name = editText.text.toString()
        ID = editText2.text.toString()
        val dateformat = "yyyy/MM/dd kk:mm"
        val df = SimpleDateFormat(dateformat)
        val mCal: Calendar = Calendar.getInstance()
        val today: String = df.format(mCal.time)
        date = today

        // websocket initialization
        val webURL = "ws://asr.iptnet.net:8080/client/ws/speech?content-type=audio/x-raw,+layout=interleaved,+rate=16000,+format=S16LE,+channels=1"
        //val webURL = "ws://echo.websocket.org"
        val listener = EchoWebSocketListener()
        val request = Request.Builder().url(webURL).build()
        //val client = OkHttpClient()
        //val client = OkHttpClient.Builder().readTimeout(3,  TimeUnit.SECONDS).build()
        val client = OkHttpClient.Builder().build()
        webSocket = client.newWebSocket(request, listener)

        // tcp socket initialization
        val thread = Thread(Runnable { setTcpSocket() })
        thread.start()

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

    // press '錄音' button to invoke this function
    private fun pressButton() {
        if(!flag) {
            flag = true
            button.text = "斷線"
            connect()
        }
        else {
            flag = false
            button.text = "連線"
            disconnect()

            // 3. close tcp socket
            val thread = Thread(Runnable { disconnectTcpSocket() })
            thread.start()
        }
    }

}
