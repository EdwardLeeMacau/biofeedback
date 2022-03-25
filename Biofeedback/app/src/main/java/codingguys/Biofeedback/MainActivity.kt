package codingguys.Biofeedback

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.PopupMenu
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.Thread.sleep
import java.util.*
import kotlin.concurrent.*

// Global variable initialize
// Bluetooth
val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
var m_bluetoothSocket: BluetoothSocket? = null
var bleReader: InputStreamReader? = null
var bleWriter: OutputStreamWriter? = null
var readStr: String = ""
var packages: List<String> = listOf<String>()

// Timer
var taskHandler = Handler()
var readBluetooth: Boolean = true
var connectToBluetooth: Boolean = false
val updatePeriod: Long = 100L

fun sendCommand(input: String) {
    if (m_bluetoothSocket != null) {
        try {
            bleWriter!!.write(input)
            bleWriter!!.flush()
        } catch (e: IOException){
            e.printStackTrace()
        }
    }
}

fun readCommand(): String {
    var readArray = CharArray(1000)
    var readStr = ""
    if (m_bluetoothSocket != null) {
        try {
           bleReader!!.read(readArray)
        } catch (e: IOException){
            e.printStackTrace()
        }
        readStr = String(readArray)
    }
    return readStr
}

// TODO: return value type (How to assign the value to packages and readStir)
fun unpack(rawMessage: String, delimiter: String): Pair<List<String>, String>{
    val packages = rawMessage.split(delimiter).dropLast(1)
    return Pair(packages, packages.last())
}

// TODO: return 4 datas (page, Condutance, HeartRate, Temperature)
fun extractPackage(pack: String): Triple<Int, Int, Float>? {
    val content = pack.replace("(", "").replace(")", "").split(",")
    try {
        return Triple(content[1].toInt(), content[2].toInt(), content[3].toFloat())
    } catch (e: Exception){
        return null
    }
}

// Handler
// TODO: Execute this function in other activities
fun handlerReceive(runnable: Runnable){
    taskHandler.postDelayed(runnable, 0)
}

// Handler
fun handlerClose(runnable: Runnable) {
    readBluetooth = false
    taskHandler.removeCallbacks(runnable)
}

class MainActivity : AppCompatActivity() {
    // Bluetooth
    var mmDevice: BluetoothDevice? = null
    var bleThread: Thread? = null
    var REQUEST_ENABLE_BT: Int = 1
    val TAG = "MY_APP_DEBUG_TAG"

    private lateinit var debugText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar_main))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        debugText = findViewById(R.id.main_showbluetooth)

        // Bluetooth Initialize
        if (!connectToBluetooth) {
            initBLE(bluetoothAdapter)
            mmDevice = query(bluetoothAdapter, "mypi")
            if (mmDevice != null) {
                bleThread = ConnectThread(mmDevice)
                bleReader = InputStreamReader(m_bluetoothSocket!!.inputStream)
                bleWriter = OutputStreamWriter(m_bluetoothSocket!!.outputStream)

                bleThread?.run()
                connectToBluetooth = true
            }
        }
    }

    // Bluetooth Function
    private inner class ConnectThread(device: BluetoothDevice?): Thread() {
        init {
            m_bluetoothSocket = device?.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
        }

        override fun run(){
            bluetoothAdapter?.cancelDiscovery()

            try {
                m_bluetoothSocket!!.connect()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun cancel() {
            try {
                m_bluetoothSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG,"Could not close the client socket", e)
            }
        }
    }

    // Bluetooth Funcion
    private fun initBLE(bluetoothAdapter: BluetoothAdapter?) {
        if (bluetoothAdapter == null) { }
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBTIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT)
        }
    }

    // Bluetooth Function
    private fun query(bluetoothAdapter: BluetoothAdapter?, targetDeviceName: String): BluetoothDevice? {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        var allDevice: String = ""

        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address
            allDevice += device.name + '\n'
        }

        return pairedDevices?.find {it.name == targetDeviceName}
    }

    // UI Function
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // UI Function
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.debug -> {
                if (item.isChecked) { item.setChecked(false) }
                else { item.setChecked(true) }
            }

            // ToolBar Menu controlling bluetooth
            R.id.disconnectBluetooth -> {
                connectToBluetooth = false
                readBluetooth = false
                // bleThread?.cancel()
            }
            R.id.connectBluetooth -> {
                if (!connectToBluetooth) {
                    initBLE(bluetoothAdapter)
                    mmDevice = query(bluetoothAdapter, "mypi")
                    if (mmDevice != null) {
                        bleThread = ConnectThread(mmDevice)
                        bleReader = InputStreamReader(m_bluetoothSocket!!.inputStream)
                        bleWriter = OutputStreamWriter(m_bluetoothSocket!!.outputStream)

                        bleThread?.run()
                    }
                }
                connectToBluetooth = true
                readBluetooth = true
                handlerReceive(runnable)
            }
            else -> {}
        }

        return true
    }

    // UI Function
    fun practice(view: View) {
        val menu = PopupMenu(this, view)
        menu.inflate(R.menu.menu_practice)
        menu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item: MenuItem? ->
            when (item!!.itemId) {
                R.id.heartrateOption -> {
                    sendCommand("2 3")

                    val intent = Intent(this, MindfulnessActivity::class.java)
                    val mode: String = "HeartRate"
                    intent.putExtra("Mode", mode)
                    startActivity(intent)
                }
                R.id.temperatureOption -> {
                    sendCommand("3 4")

                    val intent = Intent(this, MindfulnessActivity::class.java)
                    val mode: String = "Temperature"
                    intent.putExtra("Mode", mode)
                    startActivity(intent)
                }
                else -> {}
            }
            true
        })
        menu.show()
    }

    // UI Function
    fun changeToRecordLayout(view: View) {
        val intent = Intent(this, RecordActivity::class.java)

        intent.putExtra("QueryRecord", "Previous")
        intent.putExtra("recordTemp", floatArrayOf())
        intent.putExtra("recordCond", floatArrayOf())
        intent.putExtra("recordHeartRate", floatArrayOf())
        startActivity(intent)
    }

    // UI Function
    fun changeToTrainingLayout(view: View) {
        Toast.makeText(applicationContext, "Coming Soon", Toast.LENGTH_SHORT).show()
    }

    private var runnable = object: Runnable{
        override fun run() {
            if (readBluetooth && connectToBluetooth) {
                // readStr += readCommand()
                // taskHandler.postDelayed(this, updatePeriod)

                // Deprecated
                // TODO: Check the bluetooth function runs well and switch to the above codes.
                debugText.text = readCommand()
                taskHandler.postDelayed(this, 100)
            }
        }
    }
}