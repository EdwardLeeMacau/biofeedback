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
import com.codingguys.androidfeedback.Progress
import com.codingguys.androidfeedback.R
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.*
import java.util.logging.Logger

// Global variable initialize
// Bluetooth
val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
var m_bluetoothSocket: BluetoothSocket? = null
var bleReader: InputStreamReader? = null
var bleWriter: OutputStreamWriter? = null
var readStr: String = ""
var packages: MutableList<String> = mutableListOf<String>()

// Timer
var taskHandler =Handler()
var readBluetooth: Boolean = true
var connectToBluetooth: Boolean = false
val updatePeriod: Long = 100L

val TAG = "MY_APP_DEBUG_TAG"

// Public bluetooth function
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

// Public bluetooth function
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

fun unpack(rawMessage: String, delimiter: String): Pair<List<String>, String>{
    val newPacks = rawMessage.split(delimiter)
    val remain = newPacks.last()
    return Pair(newPacks.dropLast(1), remain)
}

fun extractPackage(pack: String): Pair<Int?, Triple<Int, Int, Float>> {
    val content = pack.replace("(", "").replace(")", "").split(",")
    return Pair(content[0].toIntOrNull(), Triple(content[2].toInt(), content[3].toInt(), content[4].toFloat()))
}
// TODO: There is some delay in this funcion, but the updatePeroid is set as 100 ms already.


// Handler
fun handlerReceive(runnable: Runnable){
    readBluetooth = true
    taskHandler.postDelayed(runnable, 0)
    Log.d("Handler", "Receive")
}

// Handler
fun handlerClose(handler: Handler, runnable: Runnable) {
    readBluetooth = false
    handler.removeCallbacks(runnable)
    Log.d("Handler", "Close")
}


class MainActivity : AppCompatActivity() {
    //    // Bluetooth
    var mmDevice: BluetoothDevice? = null
    // debug TextView
    lateinit var debugText: TextView
    var bleThread: Thread? = null
    var REQUEST_ENABLE_BT: Int = 1
    // UI

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Constant setting
        debugText = findViewById(R.id.main_showbluetooth)
        // clear up the Handler
        taskHandler.removeCallbacksAndMessages(null)
        // Bluetooth Initialize

        // connect to BLE and execute
        handlerReceive(bluetoothRunnable)
    }
/////////////////////////////////////////// Bluetooth Subfunction //////////////////////////////////////////////////////
    // Bluetooth Funcion
    private fun initBLE(bluetoothAdapter: BluetoothAdapter?) {
        if (bluetoothAdapter == null) { }
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBTIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT)
        }
    }

    // Find all paired device and connect to "targetDevice"
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

/////////////////////////////////////////////////// UI Function ////////////////////////////////////
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
                Toast.makeText(applicationContext, "Not allow to disconnect the bluetooth.", Toast.LENGTH_SHORT).show()
                // connectToBluetooth = false
                // readBluetooth = false
                //bleThread?.cancel()
            }

            // Call when fail to connectBluetooth at the App starting time
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
                    connectBluetooth = true
                    readBluetooth = true

                }
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
                    sendCommand("1 4")

                    val intent = Intent(this, MindfulnessActivity::class.java)
                    val mode: String = "HeartRate"
                    intent.putExtra("Mode", mode)
                    startActivity(intent)
                    handlerClose( taskHandler, bluetoothRunnable)
                    finish()
                }
                R.id.temperatureOption -> {
                    sendCommand("2 4")

                    val intent = Intent(this, MindfulnessActivity::class.java)
                    val mode: String = "Temperature"
                    intent.putExtra("Mode", mode)
                    startActivity(intent)
                    handlerClose( taskHandler, bluetoothRunnable)
                    finish()
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
////////////////////////////////////////// Runnable //////////////////////////////////////////
    var bluetoothRunnable = object: Runnable{
        override fun run() {
            if(connectToBluetooth && readBluetooth){
                debugText.text = readCommand()
                taskHandler.postDelayed(this, updatePeriod)
                Log.d("HAHAHAHA", "Run!")
            }
        }
    }
    // Only for flush the buffer

}