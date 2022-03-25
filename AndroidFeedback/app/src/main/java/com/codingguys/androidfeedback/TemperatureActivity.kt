package codingguys.Biofeedback

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.codingguys.androidfeedback.R
import org.w3c.dom.Text
import java.lang.Exception

class TemperatureActivity : AppCompatActivity() {
    // Constance setting
    private var previousTemp: Float = 0f
    private var presentTemp: Float = 0f
    private var stableTemp:Float = 0f
    private var recordTemp: MutableList<Float> = mutableListOf()
    private var recordCond: MutableList<Int> = mutableListOf()
    private var recordHeartRate: MutableList<Int> = mutableListOf()

    // UI
    lateinit var image: ImageView
    lateinit var debugText: TextView
    private var ballonMovementScaler: Float = 100f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temperature)

        supportActionBar?.title = "Temperature"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Constant setting
        // TODO: Make a stable algorithm to fix the rising problem
        val intent: Intent = getIntent()
        stableTemp = intent.getFloatExtra("stableTemperature", 0f)
        presentTemp = stableTemp
        previousTemp = stableTemp

        // UI setting
        image = findViewById(R.id.imageView_hotballoon)
        debugText = findViewById(R.id.temperature_debug)

        // Timer setting
        taskHandler.removeCallbacksAndMessages(null)
        handlerReceive(bluetoothRunnable)
        handlerReceive(tempRunnable)
    }

    // UI
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // UI
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.debug -> {
                if (item.isChecked) {
                    item.setChecked(false)
                }
                else {
                    item.setChecked(true)
                }
            }
            else -> { }
        }
        return true
    }

    // UI
    // TODO: Make sure the image can raise to the top in 3mins.
    private fun ballonManager(imageView: ImageView){

        if (previousTemp < presentTemp && presentTemp>stableTemp) {
            imageView.setY(imageView.getTop() - ((presentTemp-stableTemp)*ballonMovementScaler))
            Log.d("Ballon", imageView.getTop().toString())
        }
    }


    // Polymorphism
    private fun getTemperature(temp: Float){
        previousTemp = presentTemp
        presentTemp = temp
    }

    // UI
    private fun showTemperature(textView: TextView = debugText, others: String = ""){
        textView.text = (packages.size.toString() + ", " +  previousTemp.toString() + ", " +  presentTemp.toString() + "\n" + others)
    }

    fun skipTemperature(view: View) {
        // Kill the timer when go to the next page
        handlerClose(taskHandler, tempRunnable)
        handlerClose(taskHandler,bluetoothRunnable)
        val intent = Intent(this, RecordActivity::class.java)
        intent.putExtra("QueryRecord", "")
        intent.putExtra("recordTemp", recordTemp.toFloatArray())
        intent.putExtra("recordCond", recordCond.toIntArray())
        intent.putExtra("recordHeartRate", recordHeartRate.toIntArray())
        startActivity(intent)
    }
    private var bluetoothRunnable = object: Runnable{
        override fun run() {
            connectToBluetooth = true
            readBluetooth = true
            if (readBluetooth && connectToBluetooth) {
                readStr += readCommand()
                if (readStr.length > 0) {

                    val (newPackages, remain) = unpack(readStr, ";")
                    packages.addAll(newPackages)
                    readStr = remain
                }
            }
            taskHandler.postDelayed(this, updatePeriod)
        }
    }
    private val tempRunnable = object : Runnable {
        override fun run() {
            if (packages.size > 0) {
                val pack = packages.get(0)
                val (page: Int?, bioSignal: Triple<Int, Int, Float>) = extractPackage(pack)
                val cond = bioSignal.first
                val hr = bioSignal.second
                val temp = bioSignal.third

                recordCond.add(cond)
                recordHeartRate.add(hr)
                recordTemp.add(temp)

                packages.removeAt(0)
                if (pack != "") {
                    try {
                        getTemperature(temp)
                        showTemperature(debugText, pack)
                    } catch (e: Exception) {
                        debugText.text = e.toString()
                    }
                }

            }
            ballonManager(image)
            taskHandler.postDelayed(this, updatePeriod)
        }
    }
}